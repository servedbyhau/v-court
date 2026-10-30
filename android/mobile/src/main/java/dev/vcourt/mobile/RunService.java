package dev.vcourt.mobile;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.vcourt.engine.booking.BookingSettings;
import dev.vcourt.engine.clock.RunClock;
import dev.vcourt.engine.driver.DriverException;
import dev.vcourt.engine.driver.DriverSession;
import dev.vcourt.engine.events.Event;
import dev.vcourt.engine.events.EventKind;
import dev.vcourt.engine.events.EventSink;
import dev.vcourt.engine.events.Reporter;
import dev.vcourt.engine.flow.BookingRun;
import dev.vcourt.engine.flow.CourtBookerException;
import dev.vcourt.engine.flow.FlowClock;
import dev.vcourt.engine.flow.RoundResult;
import rikka.shizuku.Shizuku;

/**
 * Runs one booking in the foreground: open a driver session, run {@link BookingRun} with the
 * stored settings, stop the driver, record the history and post the outcome. One run at a
 * time; a second start while one runs is refused. A timed run waits for the opening time on
 * the app's home screen, as on the desktop.
 */
public final class RunService extends Service {
    static final String ACTION_RUN = "dev.vcourt.mobile.RUN";
    static final String ACTION_CANCEL = "dev.vcourt.mobile.CANCEL";
    static final String EXTRA_DRY_RUN = "dry_run";
    static final String EXTRA_TIMED = "timed";
    static final String EXTRA_TRIGGER = "trigger";

    private static final String TAG = "VCourtRun";
    /** The longest a run may hold the phone awake: the wake-up lead plus the rounds. */
    private static final long WAKE_LOCK_MS = 40 * 60 * 1000L;
    private static final AtomicBoolean RUNNING = new AtomicBoolean();

    private volatile Thread worker;
    private volatile DriverSession session;
    private PowerManager.WakeLock wakeLock;

    /** Start a run; {@code timed} waits for the opening time, else books at once. */
    static void start(Context context, boolean dryRun, boolean timed, String trigger) {
        Intent intent = new Intent(context, RunService.class).setAction(ACTION_RUN)
                .putExtra(EXTRA_DRY_RUN, dryRun).putExtra(EXTRA_TIMED, timed)
                .putExtra(EXTRA_TRIGGER, trigger);
        context.startForegroundService(intent);
    }

    /** Tell whether a run is in progress. */
    static boolean running() {
        return RUNNING.get();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notifier notifier = new Notifier(this);
        String action = intent == null ? null : intent.getAction();
        if (ACTION_CANCEL.equals(action)) {
            cancel();
            return START_NOT_STICKY;
        }
        // Every start must go to the foreground, even one refused below.
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(Notifier.RUNNING_ID, notifier.running("Starting"),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(Notifier.RUNNING_ID, notifier.running("Starting"));
        }
        if (intent == null || !RUNNING.compareAndSet(false, true)) {
            Log.w(TAG, "A run is already in progress; this start is ignored");
            if (!RUNNING.get()) {
                stopSelf(startId);
            }
            return START_NOT_STICKY;
        }
        boolean dryRun = intent.getBooleanExtra(EXTRA_DRY_RUN, true);
        boolean timed = intent.getBooleanExtra(EXTRA_TIMED, false);
        String trigger = intent.getStringExtra(EXTRA_TRIGGER);
        PowerManager power = getSystemService(PowerManager.class);
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VCourt:run");
        wakeLock.acquire(WAKE_LOCK_MS);
        worker = new Thread(() -> {
            try {
                runOnce(notifier, dryRun, timed, trigger == null ? "manual" : trigger);
            } finally {
                RUNNING.set(false);
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
            }
        }, "booking-run");
        worker.start();
        return START_NOT_STICKY;
    }

    private void cancel() {
        Thread running = worker;
        DriverSession open = session;
        if (open != null) {
            open.cancel();
        }
        if (running != null) {
            running.interrupt();
        }
    }

    private void runOnce(Notifier notifier, boolean dryRun, boolean timed, String trigger) {
        BookingSettings settings = new SettingsStore(this).load();
        boolean english = "en".equals(settings.notificationLanguage);
        HistoryStore history = new HistoryStore(this);
        EventSink progress = event -> showProgress(notifier, event);
        Reporter report = new Reporter(history, progress, RunService::log);
        report.report(EventKind.RUN_STARTED, "trigger", trigger, "dry_run", dryRun);
        List<RoundResult> results = new ArrayList<>();
        String error = null;
        try {
            error = book(settings, dryRun, timed, report, results);
        } catch (InterruptedException e) {
            error = "Cancelled";
        } catch (CourtBookerException | DriverException | RuntimeException e) {
            Log.e(TAG, "Run failed", e);
            error = e.getMessage() == null ? e.toString() : e.getMessage();
        } finally {
            DriverSession open = session;
            session = null;
            if (open != null) {
                DriverSession.StopReport stopped = open.stop();
                Log.i(TAG, "Driver: " + CheckSupport.describe(stopped));
            }
        }
        RunSummary summary = RunSummary.of(results, dryRun, error, english);
        report.report(EventKind.RUN_FINISHED, "outcome", summary.outcome, "detail",
                error == null ? summary.title : error);
        notifier.result(summary.title, summary.text);
        history.close();
    }

    /** Return null when the rounds ran, else why the run could not book at all. */
    private String book(BookingSettings settings, boolean dryRun, boolean timed,
            Reporter report, List<RoundResult> results)
            throws CourtBookerException, DriverException, InterruptedException {
        List<String> problems = settings.validate();
        if (!problems.isEmpty()) {
            return "Settings: " + String.join("; ", problems);
        }
        if (!Shizuku.pingBinder()) {
            return "Shizuku is not running: start it, then run again";
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            return "V Court may not use Shizuku: open V Court and authorize it";
        }
        RunClock clock = new RunClock(settings.ntpServer);
        // Measured before waiting: the seconds lost when every server fails cost no booking.
        clock.measure();
        LocalDateTime start = null;
        if (timed) {
            start = RunClock.resolveStart(BookingSettings.parseTime(settings.startAt, "start_at"),
                    clock.now(), RunClock.LATE_START_GRACE);
        }
        String version;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }
        ShizukuDriverLauncher launcher = new ShizukuDriverLauncher();
        session = DriverSession.open(launcher, new LocalSocketConnector(), version);
        report.report(EventKind.DEVICE_READY, "driver", session.hello().version());
        BookingRun run = new BookingRun(new DriverDevice(session.client(), launcher), settings,
                report, FlowClock.SYSTEM, clock, seconds -> Thread.sleep(
                        (long) (seconds * 1000), (int) ((seconds * 1e9) % 1_000_000)));
        run.run(dryRun, start, results);
        return null;
    }

    private static void showProgress(Notifier notifier, Event event) {
        switch (event.kind) {
            case APP_READY:
                notifier.updateRunning("Vinhomes ready");
                break;
            case WAITING:
                notifier.updateRunning("Waiting for " + event.data.get("until"));
                break;
            case SLOT_STARTED:
                notifier.updateRunning("Booking " + event.data.get("slot"));
                break;
            default:
                break;
        }
    }

    /** Write each event to logcat as one line, as the desktop's {@code LogSink}. */
    private static void log(Event event) {
        Log.i(TAG, event.kind.value() + " " + event.data);
    }
}
