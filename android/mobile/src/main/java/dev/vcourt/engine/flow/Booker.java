package dev.vcourt.engine.flow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.vcourt.engine.booking.AppLanguage;
import dev.vcourt.engine.booking.BookingSettings;
import dev.vcourt.engine.booking.Catalog;
import dev.vcourt.engine.booking.Labels;
import dev.vcourt.engine.events.EventKind;
import dev.vcourt.engine.events.Reporter;
import dev.vcourt.engine.screen.Box;
import dev.vcourt.engine.screen.Geometry;
import dev.vcourt.engine.screen.Match;
import dev.vcourt.engine.screen.Node;
import dev.vcourt.engine.screen.Screen;

/**
 * The booking flow, one method per app screen; one {@link #book} call per round. Ported from
 * the desktop's {@code flow.py} (develop {@code 9a8e088}), whose comments give the field
 * measurements behind each constant.
 *
 * <p>Each screen is read with one dump, which lists every element with its bounds in about the
 * time of a single element query; elements are found by label in that dump, so waiting for the
 * next screen and locating the next element are the same query: a step costs one dump per
 * poll plus one tap. No coordinates are kept across screens. Device commands go only through
 * {@link #snapshot}, {@link #tap}, {@link #tapPoint}, {@link #pressBack} and the scroll, each
 * counted in {@link #commands}.
 */
public final class Booker {
    private static final Logger LOG = Logger.getLogger(Booker.class.getName());

    static final double POLL_INTERVAL = 0.02;
    /** How long a screen may take to show after Back before Back is pressed again. */
    static final double NEXT_SCREEN_TIMEOUT = 2.0;
    /**
     * How long a tap may go without the next screen before its element, if still shown, is
     * tapped again: the app swallowed it. The calendar stayed 2.3 s after a "Tiếp tục" the app
     * took, so a shorter wait would send it twice at 06:00. Never repeated while the app loads
     * the next screen (the element gone).
     */
    static final double RETAP_AFTER = 2.0;
    /** Courts opened by mistake before the round gives up. */
    static final int COURT_ATTEMPTS = 5;
    /** A court list still shown this long after a court tap, with the court moved, missed it. */
    static final double MOVED_COURT_WAIT = 0.6;
    static final int BACK_PRESSES = 8;
    /** How long the slot list is read again while "Tiếp tục" shows disabled. */
    static final double CONTINUE_WAIT = 2.0;
    /** Pause after the slot tap, before "Tiếp tục": the app takes the slot frames later. */
    static final double SLOT_SETTLE = 0.1;
    /** Least time from the checkbox tap to "Xác nhận": the app takes the tick frames later. */
    static final double TERMS_SETTLE = 0.2;
    /**
     * How long "Xác nhận" may go without the ticket or a notice before it is tapped again, if
     * the page still shows it enabled: about twice the slowest answer at 06:00.
     */
    static final double CONFIRM_RETAP_AFTER = 3.0;
    static final double FIRST_DATE_WAIT = 1.5;
    static final double NEXT_DATE_WAIT = 0.8;
    /** How long the calendar's prompt may stay after a day tap before the day is tapped again. */
    static final double DATE_RETAP_AFTER = 0.5;
    static final double CHECKBOX_TIMEOUT = 0.8;
    static final double SETTLE_TIMEOUT = 1.0;
    static final double SWIPE_SECONDS = 0.25;
    /** A slot clipped to less than this share of a full slot's height is scrolled into view. */
    static final double MIN_VISIBLE_RATIO = 0.6;

    /** Return what to act on in a screen, or null while it is not shown yet. */
    interface Finder<T> {
        T find(Screen screen);
    }

    /** What a finder found, and the screen it found it on. */
    static final class Found<T> {
        final T value;
        final Screen screen;

        Found(T value, Screen screen) {
            this.value = value;
            this.screen = screen;
        }
    }

    private enum Position { VISIBLE, BELOW, ABOVE }

    private final Device device;
    private final BookingSettings settings;
    private final Reporter report;
    private final FlowClock clock;
    private final List<AppLanguage> languages;
    private final Match anySlot = Match.pattern(Geometry.ANY_SLOT_PATTERN);
    private final Match court;
    private final Match anyCourt;
    private final Match openedCourt;

    /** Device commands sent so far; each is a round trip to the device. */
    public int commands;
    Screen lastScreen = new Screen(List.of());
    private Screen entry;
    /** The subdivision the home screen's apartment code tells, once {@link #prepare} read it. */
    public String accountSubdivision;

    private AppLanguage language;
    private Labels labels;
    private Match home;
    private Match utility;
    private Match anyDay;
    private Match prevMonth;
    private Match nextMonth;
    private Match pickDate;
    private Match continueButton;
    private Match venueList;
    private Match courtList;
    private Match residents;
    private Match agree;
    private Match confirmButton;
    private Match notice;
    private Match ticket;
    private Match processing;
    private Match registered;

    public Booker(Device device, BookingSettings settings, Reporter report, FlowClock clock) {
        this.device = device;
        this.settings = settings;
        this.report = report == null ? new Reporter() : report;
        this.clock = clock;
        court = Match.desc(settings.court).withClickable(true);
        anyCourt = Match.prefix(settings.court).withClickable(true);
        // The court named on the booking details page.
        openedCourt = Match.desc(settings.court).withClickable(false);
        // The languages the app may show; the court names read the same in each.
        languages = AppLanguage.of(settings.labels, settings.sport);
        use(languages.get(0));
    }

    /** Read and tap the app's labels in {@code language} from now on. */
    private void use(AppLanguage language) {
        this.language = language;
        labels = language.labels();
        home = Match.desc(labels.home()).withClickable(true);
        utility = Match.desc(labels.utility()).withClickable(true);
        anyDay = Match.pattern(language.anyDay());
        prevMonth = Match.desc(labels.prevMonth());
        nextMonth = Match.contains(labels.nextMonth());
        pickDate = Match.desc(labels.pickDate());
        continueButton = Match.desc(labels.continueLabel()).withClickable(true);
        venueList = Match.desc(labels.venueList());
        courtList = Match.desc(labels.courtList());
        residents = Match.contains(labels.residents());
        agree = Match.contains(labels.agree());
        confirmButton = Match.desc(labels.confirm()).withClickable(true);
        notice = Match.desc(labels.notice());
        ticket = Match.desc(labels.ticket());
        processing = Match.desc(labels.processing());
        registered = Match.desc(labels.registered());
    }

    /** Return the language the app shows, as {@link #prepare} found it. */
    public AppLanguage language() {
        return language;
    }

    // Public API -------------------------------------------------------------

    /** Turn off animations and notification banners; a failure costs only those safeguards. */
    public void quietDevice() {
        commands++;
        try {
            device.quiet();
            LOG.fine("Animations and notification banners off");
        } catch (DeviceException | RuntimeException e) {
            LOG.log(Level.WARNING, "Could not turn off animations and banners", e);
        }
    }

    /**
     * Open the app and stop on its home screen, ready for the first round. With
     * {@code restartApp} the app is stopped first, so a stale screen or expired session
     * cannot get in the way. The first screen found tells the app's language.
     */
    public void prepare() throws CourtBookerException, InterruptedException {
        String pkg = settings.appPackage;
        quietDevice();
        if (settings.restartApp) {
            LOG.info("Restarting " + pkg);
            device.stopApp(pkg);
            device.startApp(pkg);
        } else if (!pkg.equals(device.currentPackage())) {
            LOG.info("Starting " + pkg);
            device.startApp(pkg);
        }
        Found<AppLanguage> found = waitFor(this::findLanguage, settings.bootTimeout, null);
        if (found == null) {
            List<String> shown = new ArrayList<>();
            for (AppLanguage each : languages) {
                for (String label : List.of(each.labels().home(), each.labels().utility())) {
                    if (!label.isEmpty()) {
                        shown.add("'" + label + "'");
                    }
                }
            }
            String last = shown.remove(shown.size() - 1);
            throw new AppNotReadyException("The app did not show " + String.join(", ", shown)
                    + " or " + last + " in " + Math.round(settings.bootTimeout)
                    + " s; check that it is logged in");
        }
        use(found.value);
        LOG.info("App language: " + found.value.name());
        Screen homeScreen;
        try {
            homeScreen = returnToHome(found.screen);
        } catch (NavigationException e) {
            throw new AppNotReadyException(e.getMessage(), e);
        }
        readAccount(homeScreen);
        LOG.info("Ready on the home screen");
    }

    /**
     * Tell the account's subdivision from the apartment code on the home screen. The code is
     * personal data, so only the subdivision is logged and reported. Never fails the run.
     */
    void readAccount(Screen homeScreen) {
        try {
            String found = null;
            for (Node node : homeScreen.nodes()) {
                String code = node.desc.split("\n", -1)[0];
                if (node.clickable && Catalog.APARTMENT.matcher(code).matches()) {
                    found = Catalog.subdivisionOfApartment(code);
                    break;
                }
            }
            if (found == null) {
                LOG.fine("No subdivision told by the home screen");
                return;
            }
            accountSubdivision = found;
            String name = Catalog.subdivision(found).name;
            LOG.info("Account subdivision: " + name);
            report.report(EventKind.ACCOUNT_SUBDIVISION, "subdivision", found);
            if (Catalog.courtOutside(found, settings.court)) {
                LOG.warning("Court " + settings.court + " is not in " + name
                        + ", the subdivision of the account");
            }
        } catch (RuntimeException e) { // a guess never costs a booking
            LOG.log(Level.WARNING, "Could not tell the account's subdivision", e);
        }
    }

    /**
     * Open the utilities list from the home screen, or read it again if it is shown. A timed
     * run calls this before the start, so the first round taps the utility with one command.
     */
    public void locateEntry() throws CourtBookerException, InterruptedException {
        entry = returnToUtilities(null);
        LOG.info("On the utilities list ahead of the start time");
    }

    /**
     * Book {@code slot} on {@code target}, from the home or utilities screen; return what the
     * confirmation page and the ticket showed. A dry run stops before "Xác nhận".
     */
    public Booked book(String slot, LocalDate target, boolean dryRun)
            throws CourtBookerException, InterruptedException {
        Screen screen = openSlot(slot, target);
        if (screen.find(continueButton) == null) { // the slot was tapped with it disabled
            Found<Node> found = waitFor(finder(continueButton), settings.timeout, null);
            if (found == null) {
                throw new ElementNotFoundException("'" + labels.continueLabel()
                        + "' stayed disabled");
            }
            screen = found.screen;
        }
        Found<Node> next = tapAndAdvance(screen, continueButton, labels.continueLabel(),
                this::afterCalendar, false);
        Node shown = next.value;
        screen = next.screen;
        if (notice.test(shown)) {
            String message = noticeText(screen);
            throw refusal(message == null ? "" : message);
        }
        if (!courtList.test(shown) && !residents.test(shown)) {
            if (shown.desc.contains(labels.fullyBooked())) {
                throw new SlotUnavailableException("Venue " + shown.desc.split("\n", -1)[0]
                        + " is fully booked");
            }
            // Only the court list's heading counts after the venue: a read right after the tap
            // may still show the venue.
            Match venue = Match.desc(shown.desc).withClickable(true);
            next = tapAndAdvance(screen, venue, shown.desc, this::courtsOrDetails, false);
            shown = next.value;
            screen = next.screen;
        }
        if (!residents.test(shown)) {
            // A court the bottom edge cuts off is tapped as it is: the driver clicks by label.
            Found<Node> found = waitFor(this::courtOrDetails, settings.timeout, screen);
            if (found == null) {
                throw new ElementNotFoundException("Court '" + settings.court
                        + "' is not on the court list");
            }
            shown = found.value;
            screen = found.screen;
        }
        if (residents.test(shown)) {
            screen = checkSingleCourt(screen);
        } else {
            if (shown.desc.contains(labels.fullyBooked())) {
                throw new SlotUnavailableException("Court " + settings.court + " is fully booked");
            }
            screen = selectCourt(screen);
        }
        report.report(EventKind.COURT_SELECTED, "court", settings.court);
        Found<int[]> point = tapAndAdvance(screen, continueButton, labels.continueLabel(),
                this::locateCheckbox, false);
        Booked booked = readConfirmation(point.screen, slot, target);
        Screen after = acceptAndConfirm(point.value, dryRun);
        if (after != null) {
            readTicket(after, booked);
        }
        return booked;
    }

    /**
     * Open the calendar and select {@code target} and {@code slot}; return the calendar read
     * before the slot tap. Until {@code openRetrySeconds} passed, a notice in place of the
     * calendar (such as the closed-hours one) is closed with Back and the utility tapped
     * again, and a date or slot not listed yet reloads the calendar. A fully booked slot and
     * the quota notice fail at once.
     */
    Screen openSlot(String slot, LocalDate target) throws CourtBookerException,
            InterruptedException {
        double deadline = clock.now() + settings.openRetrySeconds;
        int attempt = 1;
        int notices = 0;
        Screen screen = entry;
        entry = null;
        while (true) {
            screen = returnToUtilities(screen);
            screen = tapAndAdvance(screen, utility, labels.utility(), this::calendarOrNotice,
                    notices > 0).screen;
            String message = noticeText(screen);
            if (message != null) {
                BookingRejectedException refused = refusal(message);
                if (refused instanceof QuotaFullException) {
                    throw refused;
                }
                message = message.isEmpty() ? "a notice without a message" : message;
                if (clock.now() >= deadline) {
                    throw new BookingNotOpenException(target + " " + slot + " not open after "
                            + attempt + " attempt(s): " + message);
                }
                notices++;
                LOG.log(notices == 1 ? Level.INFO : Level.FINE, "Not open yet: " + message
                        + "; closing the notice and tapping again");
                screen = goBack(screen, finder(utility));
                attempt++;
                continue;
            }
            if (notices > 0) {
                LOG.info("Calendar opened after " + attempt + " attempt(s)");
            }
            report.report(EventKind.CALENDAR_OPENED, "attempt", attempt);
            try {
                screen = selectDate(screen, target);
                return selectSlot(slot, screen);
            } catch (ElementNotFoundException e) {
                if (clock.now() >= deadline) {
                    throw new BookingNotOpenException(target + " " + slot + " not open after "
                            + attempt + " attempt(s): " + e.getMessage(), e);
                }
                LOG.info("Not open yet (" + e.getMessage() + "); reloading the calendar");
            }
            screen = null;
            attempt++;
        }
    }

    /**
     * Tap the court on the court list and return the booking details page. The list moves up
     * when the service info above it loads, so a tap placed from the read before can land on
     * another court: the details page names the court it opened, another one is left with
     * Back, and the court is tapped again where the list shows it then.
     */
    Screen selectCourt(Screen screen) throws CourtBookerException, InterruptedException {
        String name = settings.court;
        Node node = screen.find(court);
        if (node == null) {
            throw new ElementNotFoundException("Button not found: '" + name + "'");
        }
        double deadline = clock.now() + settings.timeout;
        int wrong = 0;
        while (true) {
            if (node != null) {
                tap(node);
            }
            Found<Node> found = waitFor(courtTapped(node), retapWait(deadline, RETAP_AFTER),
                    null);
            if (found == null) {
                node = lastScreen.find(court);
                LOG.fine("Next screen missing after '" + name + "'");
            } else if (found.screen.find(continueButton) == null) {
                LOG.warning("'" + name + "' moved on the court list under the tap; tapping it"
                        + " again");
                node = found.value;
            } else if (found.screen.find(openedCourt) != null) {
                LOG.info("Tapped '" + name + "'");
                return found.screen;
            } else {
                wrong++;
                LOG.warning("The app opened another court than '" + name
                        + "'; going back to the list");
                Screen back = goBack(found.screen, finder(court));
                Found<Node> list = waitFor(finder(court), NEXT_SCREEN_TIMEOUT, back);
                if (list == null) {
                    throw new NavigationException("Could not return to the court list for '"
                            + name + "'");
                }
                if (wrong >= COURT_ATTEMPTS) {
                    break;
                }
                node = list.value;
            }
            if (clock.now() >= deadline) {
                break;
            }
        }
        if (wrong > 0) {
            throw new NavigationException("The app opened another court than '" + name + "' "
                    + wrong + " time(s)");
        }
        throw new NavigationException("Tapped '" + name + "' but the next screen did not appear");
    }

    /**
     * Return the booking details page shown in place of a court list with a single court, once
     * its "Tiếp tục" is enabled; it must name the configured court.
     */
    Screen checkSingleCourt(Screen screen) throws CourtBookerException, InterruptedException {
        Found<Node> found = waitFor(this::detailsPage, settings.timeout, screen);
        if (found == null) {
            throw new ElementNotFoundException("'" + labels.continueLabel() + "' stayed disabled");
        }
        if (found.screen.find(openedCourt) == null) {
            throw new ElementNotFoundException("The app opened its only court, which is not '"
                    + settings.court + "'");
        }
        LOG.info("The app opened its only court, '" + settings.court + "'");
        return found.screen;
    }

    /** Navigate to the utilities list from the home, ticket or any inner screen. */
    Screen returnToUtilities(Screen screen) throws CourtBookerException, InterruptedException {
        if (screen == null) {
            screen = snapshot();
        }
        Finder<Node> arrived = either(utility, home);
        for (int i = 0; i < BACK_PRESSES; i++) {
            if (screen.find(utility) != null) {
                return screen;
            }
            if (screen.find(home) != null) {
                return tapAndAdvance(screen, home, labels.home(), finder(utility), false).screen;
            }
            warnAboutNotice(screen);
            screen = goBack(screen, arrived);
        }
        throw new NavigationException("Could not return to the utilities list");
    }

    /** Press Back until the home screen shows; never on it, which would exit the app. */
    Screen returnToHome(Screen screen) throws CourtBookerException, InterruptedException {
        if (screen == null) {
            screen = snapshot();
        }
        for (int i = 0; i < BACK_PRESSES; i++) {
            if (screen.find(home) != null) {
                return screen;
            }
            warnAboutNotice(screen);
            screen = goBack(screen, finder(home));
        }
        throw new NavigationException("Could not return to the home screen");
    }

    // Navigation -------------------------------------------------------------

    /**
     * Tap {@code target} on {@code screen} and return what {@code findNext} finds on the next
     * screen, awaited up to {@code timeout}. A tap the app swallowed is repeated every
     * {@link #RETAP_AFTER} while {@code target} still shows, never while the next screen loads.
     */
    <T> Found<T> tapAndAdvance(Screen screen, Match target, String name, Finder<T> findNext,
            boolean quiet) throws CourtBookerException, InterruptedException {
        Node node = screen.find(target);
        if (node == null) {
            throw new ElementNotFoundException("Button not found: '" + name + "'");
        }
        double deadline = clock.now() + settings.timeout;
        int attempt = 1;
        while (true) {
            if (node != null) {
                tap(node);
            }
            Found<T> found = waitFor(findNext, retapWait(deadline, RETAP_AFTER), null);
            if (found != null) {
                LOG.log(quiet ? Level.FINE : Level.INFO, "Tapped '" + name + "'");
                return found;
            }
            if (clock.now() >= deadline) {
                throw new NavigationException("Tapped '" + name
                        + "' but the next screen did not appear");
            }
            node = lastScreen.find(target);
            attempt++;
            LOG.fine("Next screen missing after '" + name + "' (attempt " + attempt + ")");
        }
    }

    /**
     * Press Back and return the next screen once a read shows {@code arrived}, or controls of
     * its own while a control of {@code screen} is gone. Controls are compared, not labels: the
     * status bar's labels change on their own, and a screen that only gains controls is the
     * same screen. Nothing changed in 2 s returns the last read, so Back is pressed again.
     */
    Screen goBack(Screen screen, Finder<Node> arrived) throws CourtBookerException,
            InterruptedException {
        Set<Node> before = controls(screen);
        pressBack();
        Found<Boolean> found = waitFor(shown -> {
            Set<Node> now = controls(shown);
            if (arrived.find(shown) != null || (!now.isEmpty() && !now.containsAll(before))) {
                return Boolean.TRUE;
            }
            return null;
        }, NEXT_SCREEN_TIMEOUT, null);
        return found != null ? found.screen : lastScreen;
    }

    /**
     * Read the screen until {@code find} returns a value; null after {@code timeout}.
     * {@code screen}, if given, is checked first without reading the device again.
     */
    <T> Found<T> waitFor(Finder<T> find, double timeout, Screen screen)
            throws CourtBookerException, InterruptedException {
        double deadline = clock.now() + timeout;
        while (true) {
            if (screen == null) {
                screen = snapshot();
            }
            T found = find.find(screen);
            if (found != null) {
                return new Found<>(found, screen);
            }
            if (clock.now() >= deadline) {
                return null;
            }
            clock.sleep(POLL_INTERVAL);
            screen = null;
        }
    }

    // Calendar screen --------------------------------------------------------

    /** Tap {@code target} in the calendar, switching month if needed; return the read after. */
    Screen selectDate(Screen screen, LocalDate target) throws CourtBookerException,
            InterruptedException {
        Match match = Match.pattern(language.dayPattern(target));
        int targetMonth = Geometry.monthIndex(target.getYear(), target.getMonthValue());
        Screen current = screen;
        for (int attempt = 0; attempt < 5; attempt++) {
            double wait = attempt == 0 ? FIRST_DATE_WAIT : NEXT_DATE_WAIT;
            Integer shownMonth = monthShown(screen);
            if (attempt == 0 && shownMonth != null && shownMonth != targetMonth) {
                wait = 0; // a month shows only its own days: switch at once
            }
            Found<Node> found = waitFor(finder(match), wait, current);
            if (found != null) {
                Screen after = tapDay(found.screen, match, target);
                LOG.info("Selected date " + target);
                report.report(EventKind.DATE_SELECTED, "date", target.toString());
                return after;
            }
            Integer shown = monthShown(lastScreen);
            boolean forward = shown == null ? attempt % 2 == 0 : shown < targetMonth;
            switchMonth(lastScreen, forward);
            current = null;
        }
        throw new ElementNotFoundException("Date " + target + " is not bookable");
    }

    /**
     * Tap the day once it stands still, and again every {@link #DATE_RETAP_AFTER} while the
     * calendar's prompt stays: a tap on a day of a month still sliding in only stops the
     * slide. Return the screen without the prompt.
     */
    private Screen tapDay(Screen screen, Match match, LocalDate target)
            throws CourtBookerException, InterruptedException {
        double deadline = clock.now() + settings.timeout;
        int taps = 0;
        while (true) {
            Node node = stillDay(screen, match);
            if (node != null) {
                tap(node);
                taps++;
            }
            Found<Boolean> found = waitFor(this::dayTaken, retapWait(deadline, DATE_RETAP_AFTER),
                    null);
            if (found != null) {
                if (taps > 1) {
                    LOG.info("Date " + target + " taken after " + taps + " taps");
                }
                return found.screen;
            }
            if (clock.now() >= deadline) {
                throw new ElementNotFoundException("Date " + target + " not taken after " + taps
                        + " tap(s)");
            }
            screen = lastScreen;
        }
    }

    /** Return the day once it is clickable (it is not while it slides in), else as it is. */
    private Node stillDay(Screen screen, Match match) throws CourtBookerException,
            InterruptedException {
        double deadline = clock.now() + SETTLE_TIMEOUT;
        while (true) {
            Node node = screen.find(match);
            if (node == null || node.clickable || clock.now() >= deadline) {
                return node;
            }
            clock.sleep(POLL_INTERVAL);
            screen = snapshot();
        }
    }

    private Boolean dayTaken(Screen screen) {
        return screen.find(pickDate) == null ? Boolean.TRUE : null;
    }

    /**
     * Tap {@code slot} once the list shows it, scrolling only while it is hidden, and once
     * "Tiếp tục" shows enabled too (or the list was read again for {@link #CONTINUE_WAIT}).
     * Return the screen read before the tap.
     */
    Screen selectSlot(String slot, Screen screen) throws CourtBookerException,
            InterruptedException {
        Match match = Match.prefix(slot);
        double deadline = clock.now() + settings.timeout;
        int swipes = 0;
        Double disabledSince = null;
        while (true) {
            double remaining = Math.max(0, deadline - clock.now());
            Found<Node> found = waitFor(finder(anySlot), remaining, screen);
            if (found == null) {
                break;
            }
            screen = found.screen;
            Node node = screen.find(match);
            boolean down;
            if (node == null) {
                down = true;
            } else {
                if (node.desc.contains(labels.fullyBooked())) {
                    throw new SlotUnavailableException("Slot " + slot + " is fully booked");
                }
                Position position = visibility(screen, node);
                if (position == Position.VISIBLE) {
                    double now = clock.now();
                    if (screen.find(continueButton) == null) {
                        if (disabledSince == null) {
                            disabledSince = now;
                            LOG.fine("'" + labels.continueLabel()
                                    + "' disabled; reading the slot list again");
                        }
                        if (now - disabledSince < CONTINUE_WAIT) {
                            clock.sleep(POLL_INTERVAL);
                            screen = null;
                            continue;
                        }
                        LOG.warning("'" + labels.continueLabel() + "' still disabled after "
                                + CONTINUE_WAIT + " s; tapping the slot anyway");
                    } else if (disabledSince != null) {
                        LOG.info("'" + labels.continueLabel() + "' enabled after "
                                + (now - disabledSince) + " s");
                    }
                    tap(node);
                    clock.sleep(SLOT_SETTLE);
                    LOG.info("Selected slot " + slot + " (" + swipes + " swipe(s))");
                    report.report(EventKind.SLOT_SELECTED, "slot", slot, "swipes", swipes);
                    return screen;
                }
                down = position == Position.BELOW;
            }
            if (swipes >= settings.maxSwipes || clock.now() >= deadline) {
                break;
            }
            scroll(screen, down);
            swipes++;
            screen = settledScreen();
        }
        throw new ElementNotFoundException("Slot " + slot + " not found after " + swipes
                + " swipe(s)");
    }

    // Confirmation screen ----------------------------------------------------

    /**
     * Return the tap point of the consent checkbox, or null if not rendered: it has no label;
     * it is the small node beside its caption, preferring clickable and smaller nodes.
     */
    int[] locateCheckbox(Screen screen) {
        Node caption = screen.find(agree);
        if (caption == null) {
            return null;
        }
        int width = screen.width();
        Node best = null;
        for (Node node : screen.nodes()) {
            if (!Geometry.isCheckboxBeside(node.box, caption.box, width)) {
                continue;
            }
            if (best == null || (node.clickable && !best.clickable)
                    || (node.clickable == best.clickable && area(node) < area(best))) {
                best = node;
            }
        }
        if (best == null) {
            return null;
        }
        LOG.fine("Checkbox found beside caption at " + best.box);
        return new int[] {best.centerX(), best.centerY()};
    }

    /**
     * Read the date, slot and court the confirmation page books. The court was checked on
     * the details page already, so a difference is only logged and the round still confirms.
     */
    Booked readConfirmation(Screen screen, String slot, LocalDate target) {
        Booked booked = new Booked();
        booked.shownCourt = screen.lineAfter(labels.bookedPlace());
        String time = screen.lineAfter(labels.bookedTime());
        Geometry.BookedTime parsed = time == null ? null : Geometry.parseBookedTime(time);
        if (parsed != null) {
            booked.shownDate = parsed.date.toString();
            booked.shownSlot = parsed.slot;
        }
        if (booked.shownDate == null || booked.shownSlot == null || booked.shownCourt == null) {
            LOG.warning("Could not read the date, slot and court on the confirmation page");
        } else if (!booked.shownDate.equals(target.toString()) || !booked.shownSlot.equals(slot)
                || !booked.shownCourt.equals(settings.court)) {
            LOG.warning("The confirmation page books " + booked.shownDate + " "
                    + booked.shownSlot + " on " + booked.shownCourt + ", not " + target + " "
                    + slot + " on " + settings.court + "; confirming anyway");
        } else {
            LOG.info("Confirmation page: " + booked.shownDate + " " + booked.shownSlot + " on "
                    + booked.shownCourt);
        }
        return booked;
    }

    /** Note the ticket's status on the first read that showed the ticket. */
    void readTicket(Screen screen, Booked booked) {
        Node status = screen.find(processing);
        if (status == null) {
            status = screen.find(registered);
        }
        booked.ticketStatus = status == null ? null : status.desc;
    }

    /**
     * Tick the consent checkbox at {@code point} and submit. The round is booked only once
     * the ticket shows; a notice fails it, and so does neither in {@code timeout}: better a
     * booking reported missing than a missing one reported booked. "Xác nhận" is tapped again
     * every {@link #CONFIRM_RETAP_AFTER} while the page still shows it enabled; a notice after
     * such a tap may answer the second tap of a booking the first made, so the outcome is then
     * unknown. Return the screen that showed the ticket, or null in a dry run.
     */
    Screen acceptAndConfirm(int[] point, boolean dryRun) throws CourtBookerException,
            InterruptedException {
        double deadline = clock.now() + settings.timeout;
        double ticked;
        Found<Node> found;
        while (true) {
            ticked = clock.now();
            tapPoint(point[0], point[1], "");
            found = waitFor(finder(confirmButton), CHECKBOX_TIMEOUT, null);
            if (found != null) {
                break;
            }
            if (clock.now() >= deadline) {
                throw new NavigationException(
                        "Checkbox tapped but the confirm button stayed disabled");
            }
            int[] again = locateCheckbox(lastScreen);
            if (again != null) {
                point = again;
            }
        }
        LOG.info("Accepted terms");
        report.report(EventKind.TERMS_ACCEPTED);
        if (dryRun) {
            LOG.info("Dry run: skipping final confirmation");
            return null;
        }
        clock.sleep(Math.max(0, ticked + TERMS_SETTLE - clock.now()));
        Node button = found.value;
        String name = button.desc;
        deadline = clock.now() + settings.timeout;
        int taps = 0;
        double tapped = 0;
        Found<Boolean> outcome;
        while (true) {
            if (button != null) {
                if (taps > 0) {
                    LOG.info("'" + name + "' tapped again " + (clock.now() - tapped)
                            + " s after the last tap with the page unchanged (attempt "
                            + (taps + 1) + ")");
                }
                tapped = clock.now();
                tap(button);
                taps++;
            }
            outcome = waitFor(this::afterConfirm, retapWait(deadline, CONFIRM_RETAP_AFTER), null);
            if (outcome != null) {
                break;
            }
            if (clock.now() >= deadline) {
                throw new BookingUnconfirmedException("Outcome unknown: neither the ticket nor a"
                        + " notice showed " + Math.round(settings.timeout)
                        + " s after confirming (taps on '" + name + "': " + taps
                        + "); check the app");
            }
            button = lastScreen.find(confirmButton);
        }
        String message = noticeText(outcome.screen);
        if (message != null && taps > 1) {
            throw new BookingUnconfirmedException("Outcome unknown: a notice after " + taps
                    + " taps on '" + name + "': " + (message.isEmpty() ? "no message" : message)
                    + "; an earlier tap may have booked, check the app");
        }
        if (message != null) {
            throw refusal(message);
        }
        LOG.info("Booking confirmed");
        return outcome.screen;
    }

    // Dialogs ----------------------------------------------------------------

    /**
     * Return the message of the app's notice dialog, or null. The dialog's title node spans
     * the whole dialog and holds its buttons; a node with the same label but no button inside,
     * such as a tab, is not a dialog. The message is every other non-button label inside.
     */
    String noticeText(Screen screen) {
        for (Node title : screen.findAll(notice)) {
            List<Node> inner = new ArrayList<>();
            boolean button = false;
            for (Node node : screen.nodes()) {
                if (node.box.isInside(title.box)) {
                    inner.add(node);
                    button |= node.clickable && node != title;
                }
            }
            if (!button) {
                continue;
            }
            List<String> parts = new ArrayList<>();
            for (Node node : inner) {
                if (!node.desc.isEmpty() && !node.clickable && !node.desc.equals(title.desc)) {
                    parts.add(node.desc);
                }
            }
            return String.join(" ", parts);
        }
        return null;
    }

    private void warnAboutNotice(Screen screen) {
        String message = noticeText(screen);
        if (message != null) {
            LOG.warning("Closing a notice of the app: "
                    + (message.isEmpty() ? "no message" : message));
        }
    }

    // Device commands --------------------------------------------------------

    /** Read every element of the current screen with one command. */
    Screen snapshot() throws DeviceException {
        commands++;
        Screen screen;
        try {
            screen = Screen.parse(device.dump());
        } catch (IllegalArgumentException e) {
            throw new DeviceException("The device answered with an unreadable screen", e);
        }
        if (!screen.nodes().equals(lastScreen.nodes()) && LOG.isLoggable(Level.FINE)) {
            List<String> lines = screen.describe();
            LOG.fine("Screen (" + lines.size() + " elements):\n  " + String.join("\n  ", lines));
        }
        lastScreen = screen;
        return screen;
    }

    void tap(Node node) throws DeviceException {
        tapPoint(node.centerX(), node.centerY(), node.desc);
    }

    /** Tap {@code (x, y)}; {@code label} names the element, which the driver clicks by. */
    void tapPoint(int x, int y, String label) throws DeviceException {
        commands++;
        LOG.fine("Tap " + (label.isEmpty() ? "" : "'" + label + "' at ") + "(" + x + ", " + y
                + ")");
        device.click(x, y, label);
    }

    void pressBack() throws DeviceException {
        commands++;
        LOG.fine("Press Back");
        device.back();
    }

    // Helpers ----------------------------------------------------------------

    private static Finder<Node> finder(Match match) {
        return screen -> screen.find(match);
    }

    private static Finder<Node> either(Match first, Match second) {
        return screen -> {
            Node node = screen.find(first);
            return node != null ? node : screen.find(second);
        };
    }

    /** Return the language whose home or utility button the screen shows. */
    private AppLanguage findLanguage(Screen screen) {
        for (AppLanguage each : languages) {
            for (String label : List.of(each.labels().home(), each.labels().utility())) {
                if (!label.isEmpty()
                        && screen.find(Match.desc(label).withClickable(true)) != null) {
                    return each;
                }
            }
        }
        return null;
    }

    /**
     * Return true once the ticket's title or a notice shows: a frame without labels shows
     * before either, so the confirmation page going away tells nothing.
     */
    private Boolean afterConfirm(Screen screen) {
        return screen.find(ticket) != null || noticeText(screen) != null ? Boolean.TRUE : null;
    }

    private Node findCalendar(Screen screen) {
        Node node = screen.find(anyDay);
        if (node == null) {
            node = screen.find(prevMonth);
        }
        return node != null ? node : screen.find(nextMonth);
    }

    /** Return true once the calendar or a notice, such as the closed-hours one, shows. */
    private Boolean calendarOrNotice(Screen screen) {
        return findCalendar(screen) != null || noticeText(screen) != null ? Boolean.TRUE : null;
    }

    /**
     * Find the details page's enabled "Tiếp tục", or the court moved on a list left as is: a
     * list still shown {@link #MOVED_COURT_WAIT} after the tap with the court elsewhere ignored
     * the tap, and the court is returned where it shows now.
     */
    private Finder<Node> courtTapped(Node tapped) {
        double since = clock.now();
        return screen -> {
            Node button = detailsPage(screen);
            if (button != null) {
                return button;
            }
            Node moved = screen.find(court);
            if (moved != null && tapped != null && !moved.box.equals(tapped.box)
                    && clock.now() - since >= MOVED_COURT_WAIT) {
                return moved;
            }
            return null;
        };
    }

    private Node courtsOrDetails(Screen screen) {
        Node node = screen.find(courtList);
        return node != null ? node : screen.find(residents);
    }

    private Node courtOrDetails(Screen screen) {
        Node node = screen.find(anyCourt);
        return node != null ? node : screen.find(residents);
    }

    /** Return the booking details page's "Tiếp tục" once it is enabled. */
    private Node detailsPage(Screen screen) {
        Node button = screen.find(continueButton);
        return button != null && screen.find(residents) != null ? button : null;
    }

    /**
     * Return what shows after the calendar's "Tiếp tục": the court list's heading, the venue
     * to tap, the details page's caption, or a notice's title (the quota notice shows in place
     * of the next screen).
     */
    private Node afterCalendar(Screen screen) {
        Node found = screen.find(courtList);
        if (found == null) {
            found = venueEntry(screen);
        }
        if (found == null) {
            found = screen.find(residents);
        }
        if (found == null && noticeText(screen) != null) {
            return screen.find(notice);
        }
        return found;
    }

    private BookingRejectedException refusal(String message) {
        String quota = labels.quotaFull();
        if (!quota.isEmpty() && message.contains(quota)) {
            return new QuotaFullException("Booking quota used up: " + message);
        }
        return new BookingRejectedException("The app refused the booking: "
                + (message.isEmpty() ? "no message" : message));
    }

    /**
     * Return the venue to tap below the venues' heading, once listed: a single venue whatever
     * its name; of several, the one the catalog lists the court under, else the one naming
     * {@code venueKeyword}.
     */
    private Node venueEntry(Screen screen) {
        Node heading = screen.find(venueList);
        if (heading == null) {
            return null;
        }
        List<Node> venues = new ArrayList<>();
        for (Node node : screen.nodes()) {
            if (node.clickable && !node.desc.isEmpty() && node.box.top >= heading.box.bottom) {
                venues.add(node);
            }
        }
        if (venues.size() == 1) {
            return venues.get(0);
        }
        String name = Catalog.venueOf(settings.court);
        for (Node node : venues) { // a full venue reads "BE _ Sân tennis\nHết chỗ"
            if (node.desc.split("\n", -1)[0].equals(name)) {
                return node;
            }
        }
        String keyword = settings.venueKeyword;
        for (Node node : venues) {
            if (!keyword.isEmpty() && node.desc.contains(keyword)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Tell whether a slot is visible, below or above the tappable area: dump bounds are
     * clipped to what shows, so a partly hidden slot is shorter than a full one; the sticky
     * bottom button counts as hidden area.
     */
    private Position visibility(Screen screen, Node node) {
        int height = screen.height();
        Node button = screen.find(Match.desc(labels.continueLabel()));
        int bottomLimit = button != null ? button.box.top : height;
        int fullHeight = 0;
        for (Node slot : screen.findAll(anySlot)) {
            fullHeight = Math.max(fullHeight, slot.height());
        }
        if (node.box.bottom <= bottomLimit && node.height() >= fullHeight * MIN_VISIBLE_RATIO) {
            return Position.VISIBLE;
        }
        return node.centerY() > height / 2.0 ? Position.BELOW : Position.ABOVE;
    }

    private void scroll(Screen screen, boolean down) throws DeviceException {
        int width = screen.width();
        int height = screen.height();
        double start = down ? 0.62 : 0.38;
        double end = down ? 0.38 : 0.62;
        commands++;
        LOG.fine(down ? "Swipe down the list" : "Swipe up the list");
        device.swipe((int) (width * 0.5), (int) (height * start), (int) (width * 0.5),
                (int) (height * end), Math.round(SWIPE_SECONDS * 1000));
    }

    /** Read the screen until the slot list stops moving after a swipe. */
    private Screen settledScreen() throws DeviceException, InterruptedException {
        double deadline = clock.now() + SETTLE_TIMEOUT;
        List<Box> previous = null;
        while (true) {
            Screen screen = snapshot();
            List<Box> boxes = new ArrayList<>();
            for (Node node : screen.findAll(anySlot)) {
                boxes.add(node.box);
            }
            if (boxes.equals(previous) || clock.now() >= deadline) {
                return screen;
            }
            previous = boxes;
            clock.sleep(POLL_INTERVAL);
        }
    }

    /** Return the month index of the first day the screen lists, or null without one. */
    private Integer monthShown(Screen screen) {
        Node firstDay = screen.find(anyDay);
        return firstDay == null ? null : language.monthIndexFromLabel(firstDay.desc);
    }

    private void switchMonth(Screen screen, boolean forward) throws DeviceException {
        Node button = screen.find(forward ? nextMonth : prevMonth);
        if (button != null) {
            tap(button);
        } else {
            double[] ratio = forward ? settings.nextMonthXy : settings.prevMonthXy;
            tapPoint((int) (screen.width() * ratio[0]), (int) (screen.height() * ratio[1]), "");
        }
        LOG.fine("Switched to " + (forward ? "next" : "previous") + " month");
    }

    private double retapWait(double deadline, double after) {
        return Math.min(after, Math.max(0, deadline - clock.now()));
    }

    /**
     * Return the clickable elements, except one that covers the whole screen: the frame read
     * while a screen is built has only that one.
     */
    private static Set<Node> controls(Screen screen) {
        Box whole = new Box(0, 0, screen.width(), screen.height());
        Set<Node> controls = new HashSet<>();
        for (Node node : screen.nodes()) {
            if (node.clickable && !node.box.equals(whole)) {
                controls.add(node);
            }
        }
        return controls;
    }

    private static long area(Node node) {
        return (long) node.box.width() * node.box.height();
    }
}
