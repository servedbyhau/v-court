package dev.vcourt.mobile;

import android.content.Context;

/** What follows a saved change of the settings: what depends on them is brought up to date. */
final class SettingsChanged {
    private SettingsChanged() {
    }

    static void notify(Context context) {
        // The wake-up time may have moved.
        DailySchedule.update(context);
    }
}
