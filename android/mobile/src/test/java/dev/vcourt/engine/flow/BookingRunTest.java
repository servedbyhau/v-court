package dev.vcourt.engine.flow;

import static dev.vcourt.engine.flow.BookerTest.SLOT;
import static dev.vcourt.engine.flow.BookerTest.bookingApp;
import static dev.vcourt.engine.flow.BookerTest.notice;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.vcourt.engine.booking.BookingSettings;
import dev.vcourt.engine.clock.RunClock;
import dev.vcourt.engine.events.EventKind;
import dev.vcourt.engine.events.MemorySink;
import dev.vcourt.engine.events.Reporter;

/** After the desktop's {@code tests/test_runner.py} ({@code run_booking}). */
class BookingRunTest {
    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 9, 6, 0);

    private final FakeFlowClock flowClock = new FakeFlowClock();
    private final MemorySink sink = new MemorySink();
    private final LocalDateTime[] now = {START.minusMinutes(5)};
    private final RunClock clock = new RunClock("", server -> null, () -> now[0], () -> 0);
    private final List<RoundResult> results = new ArrayList<>();

    private BookingRun run(FakeApp app, BookingSettings settings) {
        return new BookingRun(app, settings, new Reporter(sink), flowClock, clock,
                seconds -> now[0] = now[0].plusNanos(Math.round(seconds * 1e9)));
    }

    private static BookingSettings oneSlot() {
        BookingSettings settings = new BookingSettings();
        settings.slots = new ArrayList<>(List.of(SLOT));
        return settings;
    }

    @Test
    void aTimedRunWaitsOnTheHomeScreenAndBooksTheDayTwoDaysAhead() throws Exception {
        FakeApp app = bookingApp(false, "loading");

        run(app, oneSlot()).run(false, START, results);

        assertEquals(1, results.size());
        assertTrue(results.get(0).ok);
        assertEquals("booked", results.get(0).detail);
        assertTrue(!now[0].isBefore(START.plus(BookingRun.OPENING_MARGIN)));
        assertEquals(List.of(EventKind.APP_READY, EventKind.WAITING, EventKind.BOOKING_STARTED,
                EventKind.SLOT_STARTED, EventKind.CALENDAR_OPENED, EventKind.DATE_SELECTED,
                EventKind.SLOT_SELECTED, EventKind.COURT_SELECTED, EventKind.TERMS_ACCEPTED,
                EventKind.SLOT_BOOKED), sink.kinds());
        assertEquals("2026-10-11", sink.events.get(2).data.get("date"));
        assertEquals("S10 - Sân tennis", sink.events.get(9).data.get("shown_court"));
    }

    @Test
    void theQuotaNoticeSkipsTheRoundsLeft() throws Exception {
        FakeApp app = bookingApp(false, "loading");
        app.screens.put("quota", notice(BookerTest.QUOTA_MESSAGE));
        app.tap("calendar_selected", "Tiếp tục", "quota");
        BookingSettings settings = oneSlot();
        settings.slots.add("12:00 - 13:00");

        run(app, settings).run(false, null, results);

        assertEquals(2, results.size());
        assertFalse(results.get(0).ok);
        assertEquals(BookingRun.QUOTA_SKIPPED, results.get(1).detail);
        assertEquals(EventKind.SLOT_FAILED, sink.kinds().get(sink.kinds().size() - 1));
    }

    @Test
    void anUnknownOutcomeIsMarkedSoItIsNeverCalledAFailureAlone() throws Exception {
        FakeApp app = bookingApp(false, "loading");
        app.tap("confirm_ticked", "Xác nhận", "loading");

        run(app, oneSlot()).run(false, null, results);

        assertFalse(results.get(0).ok);
        assertTrue(results.get(0).unknown);
    }

    @Test
    void aFrozenDeviceEndsTheRunAfterItsRound() {
        FakeApp app = bookingApp(false, "loading");
        BookingSettings settings = oneSlot();
        settings.slots.add("12:00 - 13:00");
        Device frozen = new BookerTest.Forwarding(app) {
            @Override
            public void click(int x, int y, String label) throws DeviceException {
                if (label.equals("Sân Tennis")) {
                    throw new DeviceNotRespondingException("The driver did not answer click");
                }
                app.click(x, y, label);
            }
        };
        BookingRun run = new BookingRun(frozen, settings, new Reporter(sink), flowClock, clock,
                seconds -> now[0] = now[0].plusNanos(Math.round(seconds * 1e9)));

        assertThrows(DeviceNotRespondingException.class, () -> run.run(false, null, results));
        assertEquals(1, results.size());
    }

    @Test
    void aDryRunPassesWithoutConfirming() throws Exception {
        FakeApp app = bookingApp(false, "loading");

        run(app, oneSlot()).run(true, null, results);

        assertEquals("dry run passed", results.get(0).detail);
        assertFalse(app.taps.contains("Xác nhận"));
    }
}
