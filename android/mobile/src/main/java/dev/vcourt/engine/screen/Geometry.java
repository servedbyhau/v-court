package dev.vcourt.engine.screen;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure helpers for element bounds and dates, ported from the desktop's {@code geometry.py}. */
public final class Geometry {
    /** The label of any time slot, free or fully booked. */
    public static final String ANY_SLOT_PATTERN = "\\d{2}:\\d{2} - \\d{2}:\\d{2}.*";

    private static final double CHECKBOX_MAX_SIZE_RATIO = 0.2;
    private static final double CHECKBOX_GAP_RATIO = 0.02;
    private static final Pattern BOOKED_TIME =
            Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4}) (\\d{2}:\\d{2} - \\d{2}:\\d{2})");

    private Geometry() {
    }

    /**
     * Tell whether {@code box} is a plausible checkbox for the caption {@code label}: small,
     * left of the caption and on its row. The thresholds scale with the screen's width.
     */
    public static boolean isCheckboxBeside(Box box, Box label, int screenWidth) {
        double maxSize = screenWidth * CHECKBOX_MAX_SIZE_RATIO;
        double gap = screenWidth * CHECKBOX_GAP_RATIO;
        boolean small = 0 < box.width() && box.width() <= maxSize
                && 0 < box.height() && box.height() <= maxSize;
        boolean leftOfLabel = box.right <= label.left + gap;
        boolean sameRow = box.top < label.bottom && box.bottom > label.top;
        return small && leftOfLabel && sameRow;
    }

    /** Return an index that grows by one per calendar month. */
    public static int monthIndex(int year, int month) {
        return year * 12 + month;
    }

    /** A booking's date and slot, as the ticket shows them. */
    public static final class BookedTime {
        public final LocalDate date;
        public final String slot;

        BookedTime(LocalDate date, String slot) {
            this.date = date;
            this.slot = slot;
        }
    }

    /**
     * Split a booking time such as {@code 17/10/2026 15:00 - 16:00} into its date and slot;
     * the app writes it this way in Vietnamese and English. Null when it is not one.
     */
    public static BookedTime parseBookedTime(String text) {
        Matcher m = BOOKED_TIME.matcher(text.strip());
        if (!m.matches()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.of(Integer.parseInt(m.group(3)),
                    Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
            return new BookedTime(date, m.group(4));
        } catch (DateTimeException e) {
            return null;
        }
    }
}
