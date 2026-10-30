package dev.vcourt.engine.events;

/** What happened in a run. The value is stable: history rows and messages key on it. */
public enum EventKind {
    // The whole run.
    RUN_STARTED("run_started"),
    DEVICE_READY("device_ready"),
    APP_READY("app_ready"),
    ACCOUNT_SUBDIVISION("account_subdivision"),
    WAITING("waiting"),
    BOOKING_STARTED("booking_started"),
    RUN_FINISHED("run_finished"),
    // One round, i.e. one slot.
    SLOT_STARTED("slot_started"),
    CALENDAR_OPENED("calendar_opened"),
    DATE_SELECTED("date_selected"),
    SLOT_SELECTED("slot_selected"),
    COURT_SELECTED("court_selected"),
    TERMS_ACCEPTED("terms_accepted"),
    SLOT_BOOKED("slot_booked"),
    SLOT_FAILED("slot_failed"),
    // The app, outside any run.
    SETTINGS_SAVED("settings_saved"),
    SCHEDULE_ENABLED("schedule_enabled"),
    SCHEDULE_DISABLED("schedule_disabled");

    private final String value;

    EventKind(String value) {
        this.value = value;
    }

    /** Return the stable value, the desktop's. */
    public String value() {
        return value;
    }
}
