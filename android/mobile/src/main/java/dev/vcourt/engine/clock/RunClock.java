package dev.vcourt.engine.clock;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * The time a timed run waits by: NTP time carried forward on a monotonic counter that no
 * clock setting moves, as the desktop's {@code NtpClock}: the phone may set its clock while a
 * run waits. Without an answer the clock is the phone's.
 */
public final class RunClock {
    private static final Logger LOG = Logger.getLogger(RunClock.class.getName());

    /** A start time missed by less than this (the phone woke late) still runs at once. */
    public static final Duration LATE_START_GRACE = Duration.ofMinutes(10);
    static final double COARSE_STEP = 30.0;
    static final double FINE_WINDOW = 1.0;
    static final double FINE_STEP = 0.001;

    /** Sleeps the waiting thread; tests replace it. */
    public interface Sleeper {
        void sleep(double seconds) throws InterruptedException;
    }

    private final String server;
    private final Ntp.Query query;
    private final Supplier<LocalDateTime> localNow;
    private final DoubleSupplier counter;
    private LocalDateTime anchor;
    private double anchoredAt;
    /** Seconds the phone's clock was behind NTP time at the last answer; null before one. */
    public Double offset;

    /** {@code server} empty never asks a time server. */
    public RunClock(String server) {
        this(server, Ntp::query, LocalDateTime::now, () -> System.nanoTime() / 1e9);
    }

    public RunClock(String server, Ntp.Query query, Supplier<LocalDateTime> localNow,
            DoubleSupplier counter) {
        this.server = server;
        this.query = query;
        this.localNow = localNow;
        this.counter = counter;
    }

    /** Ask the time server and anchor the clock to its answer; no answer keeps the anchor. */
    public void measure() {
        if (server.isEmpty()) {
            return;
        }
        Double measured = Ntp.offset(server, query);
        if (measured == null) {
            LOG.info(offset == null ? "Using the phone's clock"
                    : "Keeping the clock offset " + offset + " s");
            return;
        }
        double at = counter.getAsDouble();
        LocalDateTime trueNow = localNow.get().plusNanos(Math.round(measured * 1e9));
        LOG.info("Clock offset " + String.format("%+.3f", measured) + " s");
        anchor = trueNow;
        anchoredAt = at;
        offset = measured;
    }

    /** Return the time carried forward from the last answer, else the phone's clock. */
    public LocalDateTime now() {
        if (anchor == null) {
            return localNow.get();
        }
        return anchor.plusNanos(Math.round((counter.getAsDouble() - anchoredAt) * 1e9));
    }

    /**
     * Return today's {@code start} as a date-time. A time passed by less than
     * {@code grace} is returned as is (the wait then ends at once).
     *
     * @throws IllegalStateException when it passed longer ago
     */
    public static LocalDateTime resolveStart(LocalTime start, LocalDateTime now,
            Duration grace) {
        LocalDateTime at = LocalDate.from(now).atTime(start);
        if (at.isBefore(now)) {
            Duration late = Duration.between(at, now);
            if (late.compareTo(grace) > 0) {
                throw new IllegalStateException("Start time " + start
                        + " has already passed today");
            }
            LOG.warning("Started " + late.getSeconds() + " s after " + start + "; booking now");
        }
        return at;
    }

    /**
     * Block until {@code now} reaches {@code target}: long sleeps first, then millisecond
     * steps for the last second, so the start is late by about a millisecond at most.
     */
    public static void waitUntil(LocalDateTime target, Supplier<LocalDateTime> now,
            Sleeper sleeper) throws InterruptedException {
        double remaining = remaining(target, now);
        if (remaining > 0) {
            LOG.info("Waiting " + Math.round(remaining) + " s until " + target.toLocalTime());
        }
        while (remaining > 0) {
            if (remaining > FINE_WINDOW) {
                sleeper.sleep(Math.min(remaining - FINE_WINDOW, COARSE_STEP));
            } else {
                sleeper.sleep(Math.min(remaining, FINE_STEP));
            }
            remaining = remaining(target, now);
        }
    }

    private static double remaining(LocalDateTime target, Supplier<LocalDateTime> now) {
        return Duration.between(now.get(), target).toNanos() / 1e9;
    }
}
