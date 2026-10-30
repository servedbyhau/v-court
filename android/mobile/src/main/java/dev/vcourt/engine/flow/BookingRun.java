package dev.vcourt.engine.flow;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.vcourt.engine.booking.BookingSettings;
import dev.vcourt.engine.clock.RunClock;
import dev.vcourt.engine.events.EventKind;
import dev.vcourt.engine.events.Reporter;
import dev.vcourt.engine.screen.Screen;

/**
 * One booking run on a ready device: prepare the app, wait for the opening time if given,
 * then book every slot. Ported from the desktop's {@code runner.run_booking}.
 */
public final class BookingRun {
    private static final Logger LOG = Logger.getLogger(BookingRun.class.getName());

    /** How long before the start time the utilities entry is read again. */
    public static final Duration LOCATE_AHEAD = Duration.ofSeconds(2);
    /**
     * How long after the start time a timed run taps first: the app answered an early tap
     * with a notice (up to 0.76 s early, 2026-10-19); retaps handle a notice.
     */
    public static final Duration OPENING_MARGIN = Duration.ofMillis(100);
    /** Why a round after the app's quota notice failed without running. */
    public static final String QUOTA_SKIPPED = "Skipped: the booking quota is used up";

    private final Device device;
    private final BookingSettings settings;
    private final Reporter report;
    private final FlowClock flowClock;
    private final RunClock clock;
    private final RunClock.Sleeper sleeper;
    private Booker booker;

    public BookingRun(Device device, BookingSettings settings, Reporter report,
            FlowClock flowClock, RunClock clock, RunClock.Sleeper sleeper) {
        this.device = device;
        this.settings = settings;
        this.report = report;
        this.flowClock = flowClock;
        this.clock = clock;
        this.sleeper = sleeper;
    }

    /** Return the booker of the run, once {@link #run} started it. */
    public Booker booker() {
        return booker;
    }

    /**
     * Prepare the app, wait for {@code start} if given (by the run clock, measured again at
     * the warm-up, until {@link #OPENING_MARGIN} after it), then book every slot on the start
     * date plus {@link BookingSettings#DAYS_AHEAD}. After the quota notice the slots left fail
     * unrun. Results are appended to {@code results} as rounds finish, so an interrupted caller
     * still sees them.
     */
    public List<RoundResult> run(boolean dryRun, LocalDateTime start, List<RoundResult> results)
            throws CourtBookerException, InterruptedException {
        booker = new Booker(device, settings, report, flowClock);
        try {
            booker.prepare();
            report.report(EventKind.APP_READY);
            if (start != null) {
                report.report(EventKind.WAITING, "until", start.withNano(0).toString());
                // Wait on the home screen, then load the utilities list shortly before the
                // start, so the app has just talked to its server when the first tap comes.
                RunClock.waitUntil(start.minusNanos(Math.round(settings.warmUpSeconds * 1e9)),
                        clock::now, sleeper);
                clock.measure();
                try {
                    booker.locateEntry();
                } catch (BookingException e) {
                    LOG.warning("Could not open the utilities list yet (" + e.getMessage()
                            + "); retrying");
                }
                RunClock.waitUntil(start.minus(LOCATE_AHEAD), clock::now, sleeper);
                booker.locateEntry();
                RunClock.waitUntil(start.plus(OPENING_MARGIN), clock::now, sleeper);
                LOG.info("Start reached at " + clock.now().toLocalTime());
            }
        } catch (CourtBookerException | RuntimeException e) {
            logLastScreen();
            throw e;
        }
        LocalDate target = (start != null ? start : clock.now()).toLocalDate()
                .plusDays(BookingSettings.DAYS_AHEAD);
        LOG.info("Target date " + target + ", slots: " + String.join(", ", settings.slots)
                + (dryRun ? " (dry run)" : ""));
        report.report(EventKind.BOOKING_STARTED, "date", target.toString(), "sport",
                settings.sport, "court", settings.court, "slots", List.copyOf(settings.slots),
                "dry_run", dryRun);
        List<String> slots = settings.slots;
        for (int index = 1; index <= slots.size(); index++) {
            String slot = slots.get(index - 1);
            LOG.info("Round " + index + "/" + slots.size() + ": " + slot);
            report.report(EventKind.SLOT_STARTED, "slot", slot, "round", index, "rounds",
                    slots.size());
            double started = flowClock.now();
            int commands = booker.commands;
            Map<String, Object> shown = Map.of();
            DeviceNotRespondingException frozen = null;
            boolean quotaFull = false;
            boolean unknown = false;
            boolean ok;
            String detail;
            try {
                shown = booker.book(slot, target, dryRun).eventData();
                ok = true;
                detail = dryRun ? "dry run passed" : "booked";
            } catch (DeviceNotRespondingException e) {
                // A frozen device answers no later round either: end the run after this one.
                logLastScreen();
                ok = false;
                detail = e.getMessage();
                frozen = e;
            } catch (BookingException e) {
                LOG.severe(e.getMessage());
                logLastScreen();
                ok = false;
                detail = e.getMessage();
                quotaFull = e instanceof QuotaFullException;
                unknown = e instanceof BookingUnconfirmedException;
            } catch (DeviceException | RuntimeException e) { // later rounds still run
                LOG.log(Level.SEVERE, "Unexpected error", e);
                logLastScreen();
                ok = false;
                detail = e.toString();
            }
            double seconds = flowClock.now() - started;
            int used = booker.commands - commands;
            results.add(new RoundResult(slot, ok, detail, seconds,
                    (String) shown.get("shown_court"), unknown));
            double rounded = Math.round(seconds * 10) / 10.0;
            if (ok) {
                Map<String, Object> data = new java.util.LinkedHashMap<>();
                data.put("slot", slot);
                data.put("seconds", rounded);
                data.put("commands", used);
                data.put("dry_run", dryRun);
                data.putAll(shown);
                report.report(EventKind.SLOT_BOOKED, data);
            } else {
                report.report(EventKind.SLOT_FAILED, "slot", slot, "reason", detail, "seconds",
                        rounded, "commands", used, "unknown", unknown);
            }
            if (frozen != null) {
                throw frozen;
            }
            if (quotaFull) {
                skipRounds(slots.subList(index, slots.size()), results, report);
                break;
            }
        }
        return results;
    }

    /** Fail {@code slots} without running them: the quota notice would refuse each one. */
    static void skipRounds(List<String> slots, List<RoundResult> results, Reporter report) {
        if (!slots.isEmpty()) {
            LOG.warning("Booking quota used up; skipping " + String.join(", ", slots));
        }
        for (String slot : new ArrayList<>(slots)) {
            results.add(new RoundResult(slot, false, QUOTA_SKIPPED, 0, null, false));
            report.report(EventKind.SLOT_FAILED, "slot", slot, "reason", QUOTA_SKIPPED,
                    "seconds", 0.0, "commands", 0, "unknown", false);
        }
    }

    /** Log every element of the last screen read, to diagnose a failure from one run. */
    private void logLastScreen() {
        if (booker == null) {
            return;
        }
        Screen last = booker.lastScreen;
        LOG.fine("Last screen read (" + last.describe().size() + " elements):\n  "
                + String.join("\n  ", last.describe()));
    }
}
