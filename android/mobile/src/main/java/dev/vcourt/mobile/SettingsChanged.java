package dev.vcourt.mobile;

import android.content.Context;

/** What follows a saved change of the settings: what depends on them is brought up to date. */
final class SettingsChanged {
    private SettingsChanged() {
    }

    static void notify(Context context) {
        // Nothing depends on the stored settings outside a run yet; the daily schedule will.
    }
}
