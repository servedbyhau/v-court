package dev.vcourt.mobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/** V Court's two notifications: a run in progress, and the outcome of the last run. */
final class Notifier {
    static final int RUNNING_ID = 1;
    static final int RESULT_ID = 2;
    private static final String RUNNING = "running";
    private static final String RESULT = "result";

    private final Context context;
    private final NotificationManager manager;

    Notifier(Context context) {
        this.context = context.getApplicationContext();
        manager = this.context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(RUNNING, "Booking run",
                NotificationManager.IMPORTANCE_LOW));
        manager.createNotificationChannel(new NotificationChannel(RESULT, "Booking result",
                NotificationManager.IMPORTANCE_HIGH));
    }

    /** Return the notification a run shows while it runs, with a Cancel action. */
    Notification running(String text) {
        Intent cancel = new Intent(context, RunService.class).setAction(RunService.ACTION_CANCEL);
        PendingIntent cancelIntent = PendingIntent.getService(context, 1, cancel,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(context, RUNNING)
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setContentTitle("V Court")
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(openApp())
                .addAction(new Notification.Action.Builder(null, "Cancel", cancelIntent).build())
                .build();
    }

    void updateRunning(String text) {
        manager.notify(RUNNING_ID, running(text));
    }

    /** Show a run's outcome until the user opens or dismisses it. */
    void result(String title, String text) {
        manager.notify(RESULT_ID, new Notification.Builder(context, RESULT)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(openApp())
                .build());
    }

    private PendingIntent openApp() {
        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE);
    }
}
