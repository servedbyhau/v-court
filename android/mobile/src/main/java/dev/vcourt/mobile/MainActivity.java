package dev.vcourt.mobile;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDateTime;
import java.util.List;

import dev.vcourt.engine.booking.BookingSettings;
import rikka.shizuku.Shizuku;

/**
 * V Court's home: whether the phone is ready to book, the daily run, what it books, a run
 * started by hand and the latest outcome. Settings, history and the driver checks have their
 * own screens.
 */
public final class MainActivity extends Activity {
    private static final int NOTIFICATIONS_REQUEST = 2;
    private static final long REFRESH_MS = 2_000;

    private SettingsStore store;
    private TextView readiness;
    private Button authorize;
    private Button allowNotifications;
    private CheckBox daily;
    private CheckBox dailyDryRun;
    private TextView nextRun;
    private TextView booking;
    private TextView lastRun;
    private Button dryRunNow;
    private Button bookNow;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            show();
            handler.postDelayed(this, REFRESH_MS);
        }
    };
    private final Shizuku.OnRequestPermissionResultListener permissionListener =
            (requestCode, grantResult) -> runOnUiThread(this::show);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new SettingsStore(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(32, 40, 32, 32);
        body.addView(Ui.title(this, "V Court"));

        body.addView(Ui.section(this, "Ready to book"));
        readiness = Ui.body(this, "");
        body.addView(readiness);
        authorize = button(body, "Authorize Shizuku", () -> {
            if (Shizuku.pingBinder()
                    && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(1);
            }
        });
        allowNotifications = button(body, "Allow notifications", () -> requestPermissions(
                new String[] {Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATIONS_REQUEST));

        body.addView(Ui.section(this, "Daily run"));
        daily = new CheckBox(this);
        daily.setText("Book every day at the opening time");
        daily.setOnClickListener(view -> {
            store.setScheduled(daily.isChecked());
            DailySchedule.update(this);
            show();
        });
        body.addView(daily);
        dailyDryRun = new CheckBox(this);
        dailyDryRun.setText("Dry run only: stop before \"Xác nhận\"");
        dailyDryRun.setOnClickListener(view -> store.setScheduledDryRun(dailyDryRun.isChecked()));
        body.addView(dailyDryRun);
        nextRun = Ui.body(this, "");
        body.addView(nextRun);

        body.addView(Ui.section(this, "Booking"));
        booking = Ui.body(this, "");
        body.addView(booking);
        button(body, "Settings", () -> startActivity(new Intent(this, SettingsActivity.class)));

        body.addView(Ui.section(this, "Run now"));
        dryRunNow = button(body, "Dry run now", () ->
                RunService.start(this, true, false, "manual"));
        bookNow = button(body, "Book now", this::confirmBookNow);

        body.addView(Ui.section(this, "Last run"));
        lastRun = Ui.body(this, "");
        body.addView(lastRun);
        button(body, "History", () -> startActivity(new Intent(this, HistoryActivity.class)));
        button(body, "Driver checks", () ->
                startActivity(new Intent(this, DriverChecksActivity.class)));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        setContentView(scroll);
        Shizuku.addRequestPermissionResultListener(permissionListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refresh);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refresh);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] results) {
        show();
    }

    /** A real booking by hand asks first: it submits, and the quota counts it. */
    private void confirmBookNow() {
        BookingSettings settings = store.load();
        new AlertDialog.Builder(this)
                .setTitle("Book now?")
                .setMessage("V Court will book " + String.join(", ", settings.slots) + " on "
                        + settings.court + " for the day after tomorrow, right away. A booking"
                        + " counts against the apartment's quota.")
                .setPositiveButton("Book", (dialog, which) ->
                        RunService.start(this, false, false, "manual"))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void show() {
        boolean shizuku = Shizuku.pingBinder();
        boolean authorized = shizuku
                && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        boolean vinhomes = getPackageManager()
                .getLaunchIntentForPackage(DriverContract.VINHOMES) != null;
        boolean notifications = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        readiness.setText(mark(shizuku) + " Shizuku running\n"
                + mark(authorized) + " V Court authorized in Shizuku\n"
                + mark(vinhomes) + " Vinhomes Resident installed\n"
                + mark(notifications) + " Notifications allowed"
                + (RunService.running() ? "\n… A run is in progress" : ""));
        authorize.setEnabled(shizuku && !authorized);
        allowNotifications.setEnabled(!notifications);

        daily.setChecked(store.scheduled());
        dailyDryRun.setChecked(store.scheduledDryRun());
        BookingSettings settings = store.load();
        LocalDateTime next = DailySchedule.nextRun(this);
        nextRun.setText(next == null ? "The daily run is off."
                : "Next: wakes " + next.toLocalDate() + " " + next.toLocalTime() + ", books at "
                        + settings.startAt + " for " + next.toLocalDate()
                                .plusDays(BookingSettings.DAYS_AHEAD) + ".");
        booking.setText(settings.court + "\n" + String.join(", ", settings.slots)
                + "\nOpening " + settings.startAt + ", the day after tomorrow");
        boolean idle = !RunService.running() && authorized;
        dryRunNow.setEnabled(idle);
        bookNow.setEnabled(idle);

        List<HistoryStore.Run> runs = new HistoryStore(this).recent(1);
        if (runs.isEmpty()) {
            lastRun.setText("No run yet.");
        } else {
            HistoryStore.Run run = runs.get(0);
            StringBuilder text = new StringBuilder(run.started + " · " + run.trigger
                    + (run.dryRun ? " · dry run" : "") + "\n"
                    + (run.outcome == null ? "in progress or interrupted" : run.outcome));
            for (String round : run.rounds) {
                text.append('\n').append(round);
            }
            lastRun.setText(text);
        }
    }

    private Button button(LinearLayout body, String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener(view -> action.run());
        body.addView(button);
        return button;
    }

    private static String mark(boolean ok) {
        return ok ? "✓" : "✗";
    }
}
