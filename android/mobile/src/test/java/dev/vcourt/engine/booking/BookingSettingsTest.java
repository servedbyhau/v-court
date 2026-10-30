package dev.vcourt.engine.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

/** The validation cases of the desktop's {@code tests/test_config.py} that apply here. */
class BookingSettingsTest {
    private static List<String> problems(Consumer<BookingSettings> change) {
        BookingSettings settings = new BookingSettings();
        change.accept(settings);
        return settings.validate();
    }

    @Test
    void theDefaultsAreValidAndTheirCourtIsInTheCatalog() {
        assertEquals(List.of(), new BookingSettings().validate());
        assertNotNull(Catalog.venueOf(BookingSettings.DEFAULT_COURT));
    }

    @Test
    void slotsMustBeSetWellFormedAndDistinct() {
        assertEquals(List.of("choose at least one slot"), problems(s -> s.slots.clear()));
        assertEquals(List.of("slot '6:00 - 7:00' must look like '18:00 - 19:00'"),
                problems(s -> s.slots = List.of("6:00 - 7:00")));
        assertEquals(List.of("a slot is listed twice"),
                problems(s -> s.slots = List.of("06:00 - 07:00", "06:00 - 07:00")));
    }

    @Test
    void sportAndSubdivisionMustBeKnownAndAgree() {
        assertTrue(problems(s -> s.sport = "golf").get(0).startsWith("sport must be one of"));
        assertEquals(List.of("subdivision must be empty or one of origami, bs"),
                problems(s -> s.subdivision = "khu-b"));
        assertEquals(List.of("BS has no multi courts"), problems(s -> {
            s.subdivision = "bs";
            s.sport = "multi";
        }));
        assertEquals(List.of(), problems(s -> s.subdivision = "bs"));
    }

    @Test
    void timesMustParseAndWakeBeforeStart() {
        assertEquals(List.of("Invalid start_at '6h'; use HH:MM or HH:MM:SS"),
                problems(s -> s.startAt = "6h"));
        assertEquals(List.of("wake_at must be before start_at"),
                problems(s -> s.wakeAt = "06:00"));
        assertEquals(LocalTime.of(6, 0, 5), BookingSettings.parseTime("06:00:05", "x"));
        assertEquals(LocalTime.of(6, 0), BookingSettings.parseTime("6:00", "x"));
        assertThrows(IllegalArgumentException.class,
                () -> BookingSettings.parseTime("24:00", "x"));
    }

    @Test
    void numbersMustBeInRange() {
        assertEquals(List.of("timeout must be positive"), problems(s -> s.timeout = 0));
        assertEquals(List.of("max_swipes must not be negative"), problems(s -> s.maxSwipes = -1));
        assertEquals(List.of("warm_up_seconds must be 5 to 600 seconds"),
                problems(s -> s.warmUpSeconds = 601));
        assertEquals(List.of("next_month_xy must be two screen ratios between 0 and 1"),
                problems(s -> s.nextMonthXy = new double[] {1.2, 0.1}));
        assertEquals(List.of("notification_language must be one of vi, en"),
                problems(s -> s.notificationLanguage = "fr"));
        assertEquals(List.of("court is empty"), problems(s -> s.court = " "));
    }

    @Test
    void aCopyIsIndependent() {
        BookingSettings settings = new BookingSettings();
        BookingSettings copy = settings.copy();

        copy.slots.add("18:00 - 19:00");
        copy.nextMonthXy[0] = 0.5;

        assertEquals(2, settings.slots.size());
        assertEquals(0.930, settings.nextMonthXy[0]);
    }
}
