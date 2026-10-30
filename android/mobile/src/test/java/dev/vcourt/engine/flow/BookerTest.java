package dev.vcourt.engine.flow;

import static dev.vcourt.engine.flow.FakeApp.label;
import static dev.vcourt.engine.flow.FakeApp.node;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.vcourt.engine.booking.BookingSettings;
import dev.vcourt.engine.events.EventKind;
import dev.vcourt.engine.events.MemorySink;
import dev.vcourt.engine.events.Reporter;

/** The key cases of the desktop's {@code tests/test_flow.py}, against the same fake app. */
class BookerTest {
    static final LocalDate TARGET = LocalDate.of(2026, 10, 11);
    static final String DAY_10 = "10, Thứ Bảy, 10 tháng 10, 2026";
    static final String DAY_11 = "11, Chủ Nhật, 11 tháng 10, 2026";
    static final String CHECKBOX = "35,1323,79,1367";
    static final String SLOT = "11:00 - 12:00";
    static final String QUOTA_MESSAGE = "Căn hộ đã hết hạn mức đăng ký tiện ích cho phép.";
    static final String TAKEN_MESSAGE = "Tiện ích đã hết chỗ, vui lòng chọn vị trí/thời gian khác";
    static final List<String> FULL_ROUND = List.of("Sân Tennis", DAY_11, SLOT, "Tiếp tục",
            "S10 - Sân tennis", "Tiếp tục", CHECKBOX, "Xác nhận");

    private final FakeFlowClock clock = new FakeFlowClock();
    private final MemorySink sink = new MemorySink();

    static FakeApp.N continueButton() {
        return node("Tiếp tục", 35, 1490, 505, 1584);
    }

    static List<FakeApp.N> notice(String message) { // read from the device on 2026-10-10
        return List.of(node("", 0, 0, 540, 1600), label("Thông báo", 0, 33, 540, 1600),
                label(message, 47, 758, 493, 817), node("Đóng", 47, 841, 493, 936),
                label("7:43 PM", 6, 5, 50, 28));
    }

    static FakeApp.N details(String court) {
        return label("Sân Tennis\nThời gian đặt lịch\n11/10/2026 11:00 - 12:00\nVị trí/dịch vụ\n"
                + court + "\nThông tin thanh toán\nTổng tiền\n0 VND", 0, 210, 540, 775);
    }

    static List<FakeApp.N> list(Object... parts) {
        List<FakeApp.N> nodes = new ArrayList<>();
        for (Object part : parts) {
            if (part instanceof FakeApp.N) {
                nodes.add((FakeApp.N) part);
            } else {
                for (Object each : (List<?>) part) {
                    nodes.add((FakeApp.N) each);
                }
            }
        }
        return nodes;
    }

    /** The desktop's {@code booking_app}: one tennis round from the utilities list. */
    static FakeApp bookingApp(boolean viaVenue, String start) {
        List<FakeApp.N> days = List.of(node(DAY_10, 381, 344, 455, 402),
                node(DAY_11, 455, 344, 529, 402));
        List<FakeApp.N> slots = List.of(node(SLOT, 277, 884, 502, 968),
                node("14:00 - 15:00\nHết chỗ", 38, 982, 263, 1066));
        List<FakeApp.N> confirmPage = List.of(label("Xác nhận đăng ký", 145, 52, 395, 89),
                node("", 0, 1292, 540, 1465), node("", 35, 1323, 79, 1367),
                label("Tôi đã hiểu và đồng ý với ", 79, 1322, 313, 1355));
        Map<String, List<FakeApp.N>> screens = new HashMap<>();
        screens.put("loading", List.of(node("", 0, 0, 540, 1600), label("4:44 PM", 6, 5, 50, 28)));
        screens.put("home", List.of(node("Tiện ích", 200, 1500, 340, 1580)));
        screens.put("utilities", List.of(node("Sân Tennis", 35, 300, 505, 360)));
        screens.put("calendar", list(days, continueButton()));
        screens.put("calendar_slots", list(days, slots, continueButton()));
        screens.put("calendar_selected", list(days, slots, continueButton()));
        screens.put("venue", List.of(label("Chọn phân khu", 35, 958, 201, 992),
                node("Origami Grand Park", 35, 1024, 505, 1134)));
        screens.put("courts", List.of(label("Chọn vị trí/dịch vụ", 35, 958, 237, 992),
                node("S10 - Sân tennis", 35, 1024, 505, 1144),
                node("S9 - Sân tennis\nHết chỗ", 35, 1157, 505, 1277)));
        screens.put("details", List.of(label("S10 - Sân tennis", 116, 975, 274, 1004),
                label("Phí đặt chỗ 0 VND\nSố lượng cư dân\n4", 35, 873, 505, 1125),
                continueButton()));
        screens.put("confirm", list(confirmPage, details("S10 - Sân tennis"),
                label("Xác nhận", 35, 1490, 505, 1584)));
        screens.put("confirm_ticked", list(confirmPage, details("S10 - Sân tennis"),
                node("Xác nhận", 35, 1490, 505, 1584)));
        screens.put("ticket", List.of(label("Chi tiết đặt lịch", 99, 52, 311, 89),
                label("Đang xử lý", 407, 154, 499, 177)));
        Map<String, String> backs = new HashMap<>();
        backs.put("utilities", "home");
        backs.put("ticket", "utilities");
        backs.put("confirm", "details");
        backs.put("confirm_ticked", "details");
        backs.put("details", "courts");
        backs.put("courts", viaVenue ? "venue" : "calendar_selected");
        backs.put("venue", "calendar_selected");
        backs.put("calendar", "utilities");
        backs.put("calendar_slots", "utilities");
        backs.put("calendar_selected", "utilities");
        FakeApp app = new FakeApp(screens, backs, start);
        app.launchesTo = "home";
        app.tap("home", "Tiện ích", "utilities")
                .tap("utilities", "Sân Tennis", "calendar")
                .tap("calendar", DAY_11, "calendar_slots")
                .tap("calendar_slots", SLOT, "calendar_selected")
                .tap("calendar_selected", "Tiếp tục", viaVenue ? "venue" : "courts")
                .tap("venue", "Origami Grand Park", "courts")
                .tap("courts", "S10 - Sân tennis", "details")
                .tap("details", "Tiếp tục", "confirm")
                .tap("confirm", CHECKBOX, "confirm_ticked")
                .tap("confirm_ticked", "Xác nhận", "ticket");
        return app;
    }

    static FakeApp bookingApp() {
        return bookingApp(false, "utilities");
    }

    private Booker booker(FakeApp app) {
        return booker(app, new BookingSettings());
    }

    private Booker booker(FakeApp app, BookingSettings settings) {
        return new Booker(app, settings, new Reporter(sink), clock);
    }

    @Test
    void bookReadsEachScreenOnceAndTapsEachElementOnce() throws Exception {
        FakeApp app = bookingApp();
        Booker booker = booker(app);

        Booked booked = booker.book(SLOT, TARGET, false);

        assertEquals(FULL_ROUND, app.taps);
        assertEquals("ticket", app.screen);
        // 8 taps plus one read per screen: start, calendar, slots, courts, details,
        // confirmation, enabled confirm button, ticket.
        assertEquals(16, booker.commands);
        assertEquals("Đang xử lý", booked.ticketStatus);
    }

    @Test
    void everyTapNamesItsElementForTheDriver() throws Exception {
        FakeApp app = bookingApp();

        booker(app).book(SLOT, TARGET, false);

        // The driver clicks by label; the consent checkbox has none and goes by position.
        List<String> expected = new ArrayList<>();
        for (String tap : FULL_ROUND) {
            expected.add(tap.equals(CHECKBOX) ? "" : tap);
        }
        assertEquals(expected, app.labels);
    }

    @Test
    void bookReportsEachStepWithoutExtraCommands() throws Exception {
        Booker booker = booker(bookingApp());

        booker.book(SLOT, TARGET, false);

        assertEquals(List.of(EventKind.CALENDAR_OPENED, EventKind.DATE_SELECTED,
                EventKind.SLOT_SELECTED, EventKind.COURT_SELECTED, EventKind.TERMS_ACCEPTED),
                sink.kinds());
        assertEquals(Map.of("attempt", 1), sink.events.get(0).data);
        assertEquals(Map.of("date", "2026-10-11"), sink.events.get(1).data);
        assertEquals(Map.of("slot", SLOT, "swipes", 0), sink.events.get(2).data);
        assertEquals(Map.of("court", "S10 - Sân tennis"), sink.events.get(3).data);
        assertEquals(16, booker.commands);
    }

    @Test
    void theSlotAndTheTickSettleBeforeTheNextTap() throws Exception {
        FakeApp app = bookingApp();
        Map<String, Double> tapped = new HashMap<>();
        Device timed = new Forwarding(app) {
            @Override
            public void click(int x, int y, String label) {
                tapped.put(label, clock.now);
                app.click(x, y, label);
            }
        };

        new Booker(timed, new BookingSettings(), new Reporter(sink), clock).book(SLOT, TARGET,
                false);

        assertTrue(tapped.get("Xác nhận") - tapped.get("") >= Booker.TERMS_SETTLE);
    }

    @Test
    void aDryRunStopsBeforeTheConfirmationAndReadsWhatItWouldBook() throws Exception {
        FakeApp app = bookingApp();

        Booked booked = booker(app).book(SLOT, TARGET, true);

        assertEquals(FULL_ROUND.subList(0, 7), app.taps);
        assertEquals("confirm_ticked", app.screen);
        assertEquals("2026-10-11", booked.shownDate);
        assertEquals(SLOT, booked.shownSlot);
        assertEquals("S10 - Sân tennis", booked.shownCourt);
        assertNull(booked.ticketStatus);
    }

    @Test
    void aFullyBookedSlotFailsTheRound() {
        FakeApp app = bookingApp();
        app.tap("calendar", DAY_11, "calendar_slots");

        assertThrows(SlotUnavailableException.class,
                () -> booker(app).book("14:00 - 15:00", TARGET, false));
    }

    @Test
    void theQuotaNoticeInPlaceOfTheCourtsIsAQuotaError() {
        FakeApp app = bookingApp();
        app.screens.put("quota", notice(QUOTA_MESSAGE));
        app.tap("calendar_selected", "Tiếp tục", "quota");

        QuotaFullException e = assertThrows(QuotaFullException.class,
                () -> booker(app).book(SLOT, TARGET, false));
        assertEquals("Booking quota used up: " + QUOTA_MESSAGE, e.getMessage());
    }

    @Test
    void aNoticeAfterTheConfirmationIsARefusal() {
        FakeApp app = bookingApp();
        app.screens.put("taken", notice(TAKEN_MESSAGE));
        app.tap("confirm_ticked", "Xác nhận", "taken");

        BookingRejectedException e = assertThrows(BookingRejectedException.class,
                () -> booker(app).book(SLOT, TARGET, false));
        assertFalse(e instanceof QuotaFullException);
        assertEquals("The app refused the booking: " + TAKEN_MESSAGE, e.getMessage());
    }

    @Test
    void neitherTicketNorNoticeLeavesTheOutcomeUnknown() {
        FakeApp app = bookingApp();
        app.tap("confirm_ticked", "Xác nhận", "loading");

        BookingUnconfirmedException e = assertThrows(BookingUnconfirmedException.class,
                () -> booker(app).book(SLOT, TARGET, false));
        assertTrue(e.getMessage().startsWith("Outcome unknown: neither the ticket nor a notice"),
                e.getMessage());
        assertEquals(1, app.taps.stream().filter("Xác nhận"::equals).count());
    }

    @Test
    void aSwallowedConfirmIsTappedAgainButANoticeThenLeavesTheOutcomeUnknown() {
        FakeApp app = bookingApp();
        app.screens.put("taken", notice(TAKEN_MESSAGE));
        app.swallow.put("Xác nhận", 1);
        app.tap("confirm_ticked", "Xác nhận", "taken");

        BookingUnconfirmedException e = assertThrows(BookingUnconfirmedException.class,
                () -> booker(app).book(SLOT, TARGET, false));
        assertTrue(e.getMessage().startsWith("Outcome unknown: a notice after 2 taps"),
                e.getMessage());
    }

    @Test
    void aSwallowedConfirmTappedAgainCanBook() throws Exception {
        FakeApp app = bookingApp();
        app.swallow.put("Xác nhận", 1);

        Booked booked = booker(app).book(SLOT, TARGET, false);

        assertEquals("Đang xử lý", booked.ticketStatus);
        assertEquals(2, app.taps.stream().filter("Xác nhận"::equals).count());
    }

    @Test
    void aSingleVenueIsTappedOnTheWayToTheCourts() throws Exception {
        FakeApp app = bookingApp(true, "utilities");

        booker(app).book(SLOT, TARGET, false);

        assertEquals("Origami Grand Park", app.taps.get(4));
        assertEquals("ticket", app.screen);
    }

    @Test
    void aCourtOpenedByMistakeIsLeftAndTheCourtTappedAgain() throws Exception {
        FakeApp app = bookingApp();
        List<FakeApp.N> wrong = new ArrayList<>(app.screens.get("details"));
        wrong.set(0, label("S9 - Sân tennis", 116, 975, 274, 1004));
        app.screens.put("details_wrong", wrong);
        app.screens.put("courts_again", app.screens.get("courts"));
        app.backsTo.put("details_wrong", "courts_again");
        app.tap("courts", "S10 - Sân tennis", "details_wrong")
                .tap("courts_again", "S10 - Sân tennis", "details");

        booker(app).book(SLOT, TARGET, false);

        assertEquals(2, app.taps.stream().filter("S10 - Sân tennis"::equals).count());
        assertEquals(List.of("back"), app.calls);
        assertEquals("ticket", app.screen);
    }

    @Test
    void aNoticeBeforeTheOpeningIsClosedAndTheUtilityTappedAgain() throws Exception {
        FakeApp app = bookingApp();
        app.screens.put("closed", notice("Ngoài giờ đăng ký"));
        app.screens.put("utilities_again", app.screens.get("utilities"));
        app.backsTo.put("closed", "utilities_again");
        app.tap("utilities", "Sân Tennis", "closed")
                .tap("utilities_again", "Sân Tennis", "calendar");

        booker(app).book(SLOT, TARGET, false);

        assertEquals(Map.of("attempt", 2), sink.events.get(0).data);
        assertEquals("ticket", app.screen);
    }

    @Test
    void aNoticeThatOutlastsTheRetryWindowEndsTheRound() {
        FakeApp app = bookingApp();
        app.screens.put("closed", notice("Ngoài giờ đăng ký"));
        app.backsTo.put("closed", "utilities");
        app.tap("utilities", "Sân Tennis", "closed");
        BookingSettings settings = new BookingSettings();
        // The fake clock moves only while the flow sleeps, and this loop finds each screen at
        // once: a window of 0 ends it at the first notice, as 60 s of real time would.
        settings.openRetrySeconds = 0;

        BookingNotOpenException e = assertThrows(BookingNotOpenException.class,
                () -> booker(app, settings).book(SLOT, TARGET, false));
        assertTrue(e.getMessage().endsWith("Ngoài giờ đăng ký"), e.getMessage());
    }

    @Test
    void anotherMonthIsSwitchedToByTheFallbackPoint() throws Exception {
        FakeApp app = bookingApp();
        app.screens.put("september", List.of(node("30, Thứ Tư, 30 tháng 9, 2026", 455, 344, 529,
                402), node("", 480, 180, 520, 220), continueButton()));
        app.tap("utilities", "Sân Tennis", "september").tap("september", "480,180,520,220",
                "calendar");

        booker(app).book(SLOT, TARGET, false);

        assertEquals(List.of("Sân Tennis", "480,180,520,220", DAY_11), app.taps.subList(0, 3));
    }

    @Test
    void aDayTapTheCalendarIgnoredIsTappedAgain() throws Exception {
        FakeApp app = bookingApp();
        List<FakeApp.N> prompt = new ArrayList<>(app.screens.get("calendar"));
        prompt.add(label("Chọn ngày dự kiến sử dụng", 35, 420, 505, 450));
        app.screens.put("calendar", prompt);
        app.swallow.put(DAY_11, 1);

        booker(app).book(SLOT, TARGET, false);

        assertEquals(2, app.taps.stream().filter(DAY_11::equals).count());
        assertEquals("ticket", app.screen);
    }

    @Test
    void aHiddenSlotIsScrolledIntoView() throws Exception {
        FakeApp app = bookingApp();
        List<FakeApp.N> hidden = new ArrayList<>(app.screens.get("calendar_slots"));
        hidden.remove(2); // the slot asked for
        app.screens.put("calendar_hidden", hidden);
        app.tap("calendar", DAY_11, "calendar_hidden");
        app.onSwipe = fake -> fake.screen = "calendar_slots";

        booker(app).book(SLOT, TARGET, false);

        assertEquals(1, app.swipes);
        assertEquals(Map.of("slot", SLOT, "swipes", 1), sink.events.get(2).data);
    }

    @Test
    void aRoundStartsFromTheTicketOfTheRoundBefore() throws Exception {
        FakeApp app = bookingApp(false, "ticket");

        booker(app).book(SLOT, TARGET, false);

        assertEquals(List.of("back"), app.calls);
        assertEquals(FULL_ROUND, app.taps);
    }

    @Test
    void prepareRestartsTheAppAndStopsOnItsHomeScreen() throws Exception {
        FakeApp app = bookingApp(false, "loading");
        Booker booker = booker(app);

        booker.prepare();

        assertEquals(List.of("app_stop", "app_start"), app.calls);
        assertEquals(1, app.quiets);
        assertEquals("home", app.screen);
        assertEquals("Vietnamese", booker.language().name());
    }

    @Test
    void prepareFailsWhenTheAppNeverShowsItsHomeScreen() {
        FakeApp app = bookingApp(false, "loading");
        app.launchesTo = "loading";

        AppNotReadyException e = assertThrows(AppNotReadyException.class,
                () -> booker(app).prepare());
        assertEquals("The app did not show 'Tiện ích', 'Sân Tennis', 'Utilities' or 'Tennis' in"
                + " 180 s; check that it is logged in", e.getMessage());
    }

    @Test
    void prepareTellsTheSubdivisionFromTheApartmentCode() throws Exception {
        FakeApp app = bookingApp(false, "loading");
        app.screens.put("home", List.of(node("Tiện ích", 200, 1500, 340, 1580),
                node("S1.0101.01\nNguyễn Văn A", 20, 100, 300, 160)));
        Booker booker = booker(app);

        booker.prepare();

        assertEquals("origami", booker.accountSubdivision);
        assertEquals(List.of(EventKind.ACCOUNT_SUBDIVISION), sink.kinds());
        assertEquals(Map.of("subdivision", "origami"), sink.events.get(0).data);
    }

    @Test
    void anEnglishAppIsBookedWithItsEnglishLabels() throws Exception {
        FakeApp app = bookingApp(false, "loading");
        Map<String, String> english = Map.ofEntries(Map.entry("Tiện ích", "Utilities"),
                Map.entry("Sân Tennis", "Tennis"),
                Map.entry(DAY_10, "10, Saturday, October 10, 2026"),
                Map.entry(DAY_11, "11, Sunday, October 11, 2026"),
                Map.entry("Tiếp tục", "Next"),
                Map.entry("14:00 - 15:00\nHết chỗ", "14:00 - 15:00\nFull"),
                Map.entry("Chọn vị trí/dịch vụ", "Select place/service"),
                Map.entry("Phí đặt chỗ 0 VND\nSố lượng cư dân\n4",
                        "Booking fee 0 VND\nNumber of residents\n4"),
                Map.entry("Tôi đã hiểu và đồng ý với ", "I have read, understood, and agree to "),
                Map.entry("Xác nhận", "Confirm"),
                Map.entry("Chi tiết đặt lịch", "Booking details"),
                Map.entry("Đang xử lý", "Processing"));
        for (Map.Entry<String, List<FakeApp.N>> screen : app.screens.entrySet()) {
            List<FakeApp.N> nodes = new ArrayList<>();
            for (FakeApp.N n : screen.getValue()) {
                nodes.add(new FakeApp.N(english.getOrDefault(n.desc, n.desc), n.box,
                        n.clickable));
            }
            screen.setValue(nodes);
        }
        Map<String, List<FakeApp.Step>> taps = new HashMap<>();
        for (Map.Entry<String, List<FakeApp.Step>> tap : app.tapsTo.entrySet()) {
            String[] parts = tap.getKey().split("\\|", 2);
            taps.put(parts[0] + "|" + english.getOrDefault(parts[1], parts[1]), tap.getValue());
        }
        app.tapsTo.clear();
        app.tapsTo.putAll(taps);
        Booker booker = booker(app);
        booker.prepare();
        app.calls.clear();

        Booked booked = booker.book(SLOT, TARGET, false);

        assertEquals("English", booker.language().name());
        assertEquals("Processing", booked.ticketStatus);
        assertEquals("Confirm", app.taps.get(app.taps.size() - 1));
    }

    /** A device that hands every call to another; tests override the call they watch. */
    abstract static class Forwarding implements Device {
        private final Device inner;

        Forwarding(Device inner) {
            this.inner = inner;
        }

        @Override
        public String dump() throws DeviceException {
            return inner.dump();
        }

        @Override
        public void click(int x, int y, String label) throws DeviceException {
            inner.click(x, y, label);
        }

        @Override
        public void back() throws DeviceException {
            inner.back();
        }

        @Override
        public void swipe(int x1, int y1, int x2, int y2, long millis) throws DeviceException {
            inner.swipe(x1, y1, x2, y2, millis);
        }

        @Override
        public void startApp(String pkg) throws DeviceException {
            inner.startApp(pkg);
        }

        @Override
        public void stopApp(String pkg) throws DeviceException {
            inner.stopApp(pkg);
        }

        @Override
        public String currentPackage() throws DeviceException {
            return inner.currentPackage();
        }

        @Override
        public void quiet() throws DeviceException {
            inner.quiet();
        }
    }
}
