package dev.vcourt.engine.booking;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What to book and how long to wait for the app: the parts of the desktop's {@code Config}
 * that the phone needs. The emulator, adb, Windows and Telegram keys stay on the desktop; one
 * phone books with one account. Mutable like the desktop's dataclass; {@link #validate} tells
 * what is wrong before settings are saved, and loading never validates, so a stored setting
 * never stops the morning run.
 */
public final class BookingSettings {
    /** The run books today + 2: the newest day, which the app opens for booking at 06:00. */
    public static final int DAYS_AHEAD = 2;
    /** Allowed {@link #warmUpSeconds}: time to load the utilities list, at most 10 min. */
    public static final double WARM_UP_MIN = 5;
    public static final double WARM_UP_MAX = 600;
    public static final String DEFAULT_COURT = "S10 - Sân tennis";
    /** Languages of V Court's own notifications. */
    public static final List<String> NOTIFICATION_LANGUAGES = List.of("vi", "en");

    private static final Pattern SLOT = Pattern.compile("\\d{2}:\\d{2} - \\d{2}:\\d{2}");
    private static final Pattern TIME = Pattern.compile("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?");

    public String appPackage = "com.vinhomes.resident";
    public List<String> slots = new ArrayList<>(List.of("11:00 - 12:00", "14:00 - 15:00"));
    /** One of {@link Catalog#SUBDIVISIONS}' ids; empty leaves it to the account. */
    public String subdivision = "";
    /** One of {@link Sports}' keys: the tile tapped on the utilities list. */
    public String sport = "tennis";
    public String court = DEFAULT_COURT;
    /**
     * Picks the venue of a court not in the {@link Catalog} when the app lists several; a
     * catalog court takes its own venue, and a single venue is tapped whatever its name.
     */
    public String venueKeyword = "Origami";
    /** Longest wait for any screen, in seconds; a screen that shows sooner is acted on. */
    public double timeout = 30;
    /** Swipes allowed to bring a hidden slot into view. */
    public int maxSwipes = 6;
    /** Seconds to wait for the app to open. */
    public double bootTimeout = 180;
    /** Stop and start the app before booking instead of reusing its current screen. */
    public boolean restartApp = true;
    /** The booking opening time, {@code HH:MM} or {@code HH:MM:SS}. */
    public String startAt = "06:00";
    /** When the daily run wakes and starts preparing; before {@link #startAt}. */
    public String wakeAt = "05:45";
    /** The time server for the run's clock; empty uses the phone's clock. */
    public String ntpServer = "time.google.com";
    /** How long to keep reloading the calendar while the date or slot is not open yet. */
    public double openRetrySeconds = 60;
    /** How long before {@link #startAt} a timed run opens the utilities list. */
    public double warmUpSeconds = 60;
    /** Language of V Court's notifications, one of {@link #NOTIFICATION_LANGUAGES}. */
    public String notificationLanguage = "vi";
    /** Fallback tap (screen ratios) when the next-month button has no label. */
    public double[] nextMonthXy = {0.930, 0.122};
    /** Fallback tap (screen ratios) when the previous-month button has no label. */
    public double[] prevMonthXy = {0.807, 0.122};
    public Labels labels = Labels.vietnamese();

    /** Return an independent copy. */
    public BookingSettings copy() {
        BookingSettings copy = new BookingSettings();
        copy.appPackage = appPackage;
        copy.slots = new ArrayList<>(slots);
        copy.subdivision = subdivision;
        copy.sport = sport;
        copy.court = court;
        copy.venueKeyword = venueKeyword;
        copy.timeout = timeout;
        copy.maxSwipes = maxSwipes;
        copy.bootTimeout = bootTimeout;
        copy.restartApp = restartApp;
        copy.startAt = startAt;
        copy.wakeAt = wakeAt;
        copy.ntpServer = ntpServer;
        copy.openRetrySeconds = openRetrySeconds;
        copy.warmUpSeconds = warmUpSeconds;
        copy.notificationLanguage = notificationLanguage;
        copy.nextMonthXy = nextMonthXy.clone();
        copy.prevMonthXy = prevMonthXy.clone();
        copy.labels = labels;
        return copy;
    }

    /**
     * Return {@code HH:MM} or {@code HH:MM:SS} as a time of day.
     *
     * @throws IllegalArgumentException naming {@code name} when it is not one
     */
    public static LocalTime parseTime(String text, String name) {
        Matcher m = TIME.matcher(text == null ? "" : text);
        if (m.matches()) {
            int hour = Integer.parseInt(m.group(1));
            int minute = Integer.parseInt(m.group(2));
            int second = m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
            if (hour < 24 && minute < 60 && second < 60) {
                return LocalTime.of(hour, minute, second);
            }
        }
        throw new IllegalArgumentException("Invalid " + name + " '" + text
                + "'; use HH:MM or HH:MM:SS");
    }

    /** Return what is wrong with these settings for booking; empty when nothing is. */
    public List<String> validate() {
        List<String> problems = new ArrayList<>();
        if (slots.isEmpty()) {
            problems.add("choose at least one slot");
        }
        for (String slot : slots) {
            if (slot == null || !SLOT.matcher(slot).matches()) {
                problems.add("slot '" + slot + "' must look like '18:00 - 19:00'");
            }
        }
        if (new HashSet<>(slots).size() != slots.size()) {
            problems.add("a slot is listed twice");
        }
        if (!Sports.isKnown(sport)) {
            problems.add("sport must be one of " + Sports.keys());
        }
        if (!subdivision.isEmpty()) {
            Catalog.Subdivision chosen = Catalog.subdivision(subdivision);
            if (chosen == null) {
                List<String> ids = new ArrayList<>();
                for (Catalog.Subdivision item : Catalog.SUBDIVISIONS) {
                    ids.add(item.id);
                }
                problems.add("subdivision must be empty or one of " + String.join(", ", ids));
            } else if (Sports.isKnown(sport) && chosen.sport(sport) == null) {
                problems.add(chosen.name + " has no " + sport + " courts");
            }
        }
        if (court.strip().isEmpty()) {
            problems.add("court is empty");
        }
        LocalTime wake = null;
        LocalTime start = null;
        try {
            wake = parseTime(wakeAt, "wake_at");
        } catch (IllegalArgumentException e) {
            problems.add(e.getMessage());
        }
        try {
            start = parseTime(startAt, "start_at");
        } catch (IllegalArgumentException e) {
            problems.add(e.getMessage());
        }
        if (wake != null && start != null && !wake.isBefore(start)) {
            problems.add("wake_at must be before start_at");
        }
        if (timeout <= 0) {
            problems.add("timeout must be positive");
        }
        if (bootTimeout <= 0) {
            problems.add("boot_timeout must be positive");
        }
        if (openRetrySeconds < 0) {
            problems.add("open_retry_seconds must not be negative");
        }
        if (maxSwipes < 0) {
            problems.add("max_swipes must not be negative");
        }
        if (warmUpSeconds < WARM_UP_MIN || warmUpSeconds > WARM_UP_MAX) {
            problems.add("warm_up_seconds must be " + (int) WARM_UP_MIN + " to "
                    + (int) WARM_UP_MAX + " seconds");
        }
        if (!NOTIFICATION_LANGUAGES.contains(notificationLanguage)) {
            problems.add("notification_language must be one of "
                    + String.join(", ", NOTIFICATION_LANGUAGES));
        }
        checkRatio(problems, "next_month_xy", nextMonthXy);
        checkRatio(problems, "prev_month_xy", prevMonthXy);
        return problems;
    }

    private static void checkRatio(List<String> problems, String name, double[] point) {
        if (point == null || point.length != 2 || point[0] < 0 || point[0] > 1 || point[1] < 0
                || point[1] > 1) {
            problems.add(name + " must be two screen ratios between 0 and 1");
        }
    }
}
