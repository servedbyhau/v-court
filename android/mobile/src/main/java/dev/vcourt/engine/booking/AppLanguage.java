package dev.vcourt.engine.booking;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.vcourt.engine.screen.Geometry;

/**
 * The labels of the app in one language, and how its calendar names a day. The app shows
 * Vietnamese or English, as set in it; both lay out every screen the same way. Court names
 * and the booked date and time read the same in both, so they are not part of a language.
 * Ported from the desktop's {@code app_language.py}.
 */
public final class AppLanguage {
    private static final List<String> ENGLISH_MONTHS = List.of("January", "February", "March",
            "April", "May", "June", "July", "August", "September", "October", "November",
            "December");

    private final String name;
    private final Labels labels;
    private final String dayLabel;
    private final List<String> months;

    /**
     * {@code dayLabel} holds {@code {day}}, {@code {weekday}}, {@code {month}} and
     * {@code {year}}, such as {@code 2, Thứ Sáu, 2 tháng 10, 2026}; the rest of it has no
     * regex syntax. {@code months} stand for {@code {month}}, January to December.
     */
    public AppLanguage(String name, Labels labels, String dayLabel, List<String> months) {
        this.name = name;
        this.labels = labels;
        this.dayLabel = dayLabel;
        this.months = List.copyOf(months);
    }

    /**
     * Return the app's languages: Vietnamese with {@code labels} (the settings'), then
     * English. Each taps the tile of {@code sport} on the utilities list, unless
     * {@code labels} names another tile.
     *
     * @throws IllegalArgumentException for an unknown sport
     */
    public static List<AppLanguage> of(Labels labels, String sport) {
        if (!Sports.isKnown(sport)) {
            throw new IllegalArgumentException("Unknown sport '" + sport + "': use one of "
                    + Sports.keys());
        }
        List<String> numbers = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            numbers.add(Integer.toString(month));
        }
        Labels vietnamese = labels.utility().isEmpty()
                ? labels.with("utility", Sports.VIETNAMESE.get(sport)) : labels;
        return List.of(
                new AppLanguage("Vietnamese", vietnamese,
                        "{day}, {weekday}, {day} tháng {month}, {year}", numbers),
                new AppLanguage("English",
                        english().with("utility", Sports.ENGLISH.get(sport)),
                        "{day}, {weekday}, {month} {day}, {year}", ENGLISH_MONTHS));
    }

    /** Return the English labels, read from the app on 2026-10-20 unless noted. */
    public static Labels english() {
        return Labels.of(Labels.vietnamese(), Map.ofEntries(
                Map.entry("home", "Utilities"),
                Map.entry("utility", "Tennis"),
                Map.entry("continue", "Next"),
                Map.entry("confirm", "Confirm"),
                Map.entry("confirm_page", "Confirm registration"),
                // Not shown: the month buttons have no label in either language.
                Map.entry("prev_month", "Previous month"),
                Map.entry("next_month", "Next month"),
                Map.entry("pick_date", "Select expected date of use"), // 2026-10-30
                // The caption reads "I have read, understood, and agree to ".
                Map.entry("agree", "I have read"),
                Map.entry("fully_booked", "Full"),
                Map.entry("venue_list", "Select subdivision"), // 2026-10-23
                Map.entry("court_list", "Select place/service"),
                Map.entry("residents", "Number of residents"),
                Map.entry("notice", "Notification"),
                // "The apartment has reached the allowed utility registration quota ..."
                Map.entry("quota_full", "registration quota"),
                Map.entry("booked_time", "Schedule time"),
                Map.entry("booked_place", "Place/service"),
                Map.entry("ticket", "Booking details"),
                Map.entry("processing", "Processing"),
                Map.entry("registered", "Registered")));
    }

    public String name() {
        return name;
    }

    public Labels labels() {
        return labels;
    }

    /** Return a regex matching the label of any calendar day. */
    public String anyDay() {
        return dayRegex("\\d{1,2}", "(?:" + anyMonth() + ")", "\\d{4}");
    }

    /** Return a regex matching the label of {@code target} only. */
    public String dayPattern(LocalDate target) {
        return dayRegex(Integer.toString(target.getDayOfMonth()),
                Pattern.quote(months.get(target.getMonthValue() - 1)),
                Integer.toString(target.getYear()));
    }

    /** Return the month index of the calendar day labelled {@code label}, or null. */
    public Integer monthIndexFromLabel(String label) {
        String regex = dayRegex("\\d{1,2}", "(?<month>" + anyMonth() + ")", "(?<year>\\d{4})");
        Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(label == null ? "" : label);
        if (!m.matches()) {
            return null;
        }
        return Geometry.monthIndex(Integer.parseInt(m.group("year")),
                months.indexOf(m.group("month")) + 1);
    }

    private String anyMonth() {
        List<String> quoted = new ArrayList<>();
        for (String month : months) {
            quoted.add(Pattern.quote(month));
        }
        return String.join("|", quoted);
    }

    private String dayRegex(String day, String month, String year) {
        return dayLabel.replace("{weekday}", ".*").replace("{month}", month)
                .replace("{year}", year).replace("{day}", day);
    }
}
