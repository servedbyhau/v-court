package dev.vcourt.mobile;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import dev.vcourt.engine.booking.BookingSettings;

/**
 * The daily run: an exact alarm at the settings' wake-up time starts a timed run, which waits
 * on the app's home screen for the opening time. {@code setAlarmClock} is exact and exempt from
 * Doze, and lets the alarm start the run's foreground service. The alarm is set again after
 * each run, after a reboot and after an update; a late start within the run clock's grace
 * still books, and a second start while a run is in progress is refused by {@link RunService}.
 */
final class DailySchedule {
    private static final String TAG = "VCourtSchedule";
    static final String ACTION_ALARM = "dev.vcourt.mobile.DAILY_RUN";

    private DailySchedule() {
    }

    /** Set the next alarm when the daily run is on, else cancel it. */
    static void update(Context context) {
        SettingsStore store = new SettingsStore(context);
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        PendingIntent fire = alarmIntent(context);
        if (!store.scheduled()) {
            alarms.cancel(fire);
            Log.i(TAG, "Daily run off");
            return;
        }
        LocalDateTime next = next(store.load(), LocalDateTime.now());
        long at = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        alarms.setAlarmClock(new AlarmManager.AlarmClockInfo(at, openApp(context)), fire);
        Log.i(TAG, "Daily run set for " + next);
    }

    /** Return the next wake-up time after {@code now}: today's if still ahead, else tomorrow's. */
    static LocalDateTime next(BookingSettings settings, LocalDateTime now) {
        LocalTime wake;
        try {
            wake = BookingSettings.parseTime(settings.wakeAt, "wake_at");
        } catch (IllegalArgumentException e) {
            wake = BookingSettings.parseTime(new BookingSettings().wakeAt, "wake_at");
        }
        LocalDateTime today = LocalDate.from(now).atTime(wake);
        return today.isAfter(now) ? today : today.plusDays(1);
    }

    /** Return the next run's time if the daily run is on, else null. */
    static LocalDateTime nextRun(Context context) {
        SettingsStore store = new SettingsStore(context);
        return store.scheduled() ? next(store.load(), LocalDateTime.now()) : null;
    }

    private static PendingIntent alarmIntent(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class).setAction(ACTION_ALARM);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent openApp(Context context) {
        return PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE);
    }

    /** Starts the daily run when its alarm fires, and sets tomorrow's. */
    public static final class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ACTION_ALARM.equals(intent.getAction())) {
                return;
            }
            SettingsStore store = new SettingsStore(context);
            if (store.scheduled()) {
                RunService.start(context, store.scheduledDryRun(), true, "scheduled");
            }
            update(context);
        }
    }

    /** Sets the alarm again after a reboot or an update, which clear it. */
    public static final class RestoreReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                    || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
                update(context);
            }
        }
    }
}
