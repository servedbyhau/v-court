package dev.vcourt.engine.clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.junit.jupiter.api.Test;

/** After the desktop's {@code tests/test_clock.py}. */
class RunClockTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 30, 5, 59, 0);

    private static byte[] reply(int mode, int stratum, double received, double sent) {
        ByteBuffer buffer = ByteBuffer.allocate(Ntp.PACKET_SIZE);
        buffer.put(0, (byte) (0x20 | mode));
        buffer.put(1, (byte) stratum);
        putTimestamp(buffer, 32, received);
        putTimestamp(buffer, 40, sent);
        return buffer.array();
    }

    private static void putTimestamp(ByteBuffer buffer, int position, double unixSeconds) {
        double ntp = unixSeconds + Ntp.EPOCH_OFFSET;
        long seconds = (long) Math.floor(ntp);
        long fraction = (long) ((ntp - seconds) * 4294967296.0);
        buffer.putInt(position, (int) seconds);
        buffer.putInt(position + 4, (int) fraction);
    }

    @Test
    void aReplyGivesTheOffsetAndTheRoundTrip() {
        // Sent at 100.0 local, the server took it at 100.6 and answered at 100.7, received
        // at 100.3 local: the local clock is 0.5 s behind, the round trip 0.2 s.
        Ntp.Sample sample = Ntp.readReply(reply(4, 2, 100.6, 100.7), 100.0, 100.3);

        assertEquals(0.5, sample.offset, 1e-6);
        assertEquals(0.2, sample.delay, 1e-6);
    }

    @Test
    void anUnsynchronisedOrShortReplyIsNoAnswer() {
        assertNull(Ntp.readReply(reply(4, 0, 1, 1), 0, 0)); // stratum 0, as in a refusal
        assertNull(Ntp.readReply(reply(3, 2, 1, 1), 0, 0)); // not from a server
        assertNull(Ntp.readReply(new byte[10], 0, 0));
    }

    @Test
    void theFastestAnswerOfTheFirstServerThatAnswersWins() {
        List<String> asked = new ArrayList<>();
        Deque<Ntp.Sample> answers = new ArrayDeque<>(List.of(new Ntp.Sample(0.4, 0.3),
                new Ntp.Sample(0.41, 0.05), new Ntp.Sample(0.5, 0.2), new Ntp.Sample(0.6, 0.4)));

        Double offset = Ntp.offset("time.example", server -> {
            asked.add(server);
            return server.equals("time.example") ? null : answers.poll();
        });

        assertEquals(0.41, offset);
        assertEquals(List.of("time.example", "time.example", "time.google.com",
                "time.google.com", "time.google.com", "time.google.com"), asked);
    }

    @Test
    void noServerAnsweringGivesNoOffset() {
        assertNull(Ntp.offset("time.google.com", server -> null));
    }

    @Test
    void theClockCarriesTheAnswerForwardOnTheCounter() {
        double[] counter = {10.0};
        LocalDateTime[] local = {NOW};
        RunClock clock = new RunClock("time.example", server -> new Ntp.Sample(0.5, 0.01),
                () -> local[0], () -> counter[0]);
        assertEquals(NOW, clock.now()); // the phone's clock before an answer

        clock.measure();
        counter[0] += 60;
        local[0] = NOW.plusSeconds(61); // the phone's clock was set meanwhile

        assertEquals(NOW.plusSeconds(60).plusNanos(500_000_000), clock.now());
        assertEquals(0.5, clock.offset);
    }

    @Test
    void anEmptyServerUsesThePhonesClock() {
        RunClock clock = new RunClock("", server -> {
            throw new AssertionError("asked");
        }, () -> NOW, () -> 0);

        clock.measure();

        assertEquals(NOW, clock.now());
    }

    @Test
    void aStartMissedByLessThanTheGraceRunsAtOnce() {
        assertEquals(NOW.withSecond(0).withMinute(55), RunClock.resolveStart(LocalTime.of(5, 55),
                NOW, RunClock.LATE_START_GRACE));
        assertEquals(NOW.toLocalDate().atTime(6, 0), RunClock.resolveStart(LocalTime.of(6, 0),
                NOW, RunClock.LATE_START_GRACE));
        assertThrows(IllegalStateException.class, () -> RunClock.resolveStart(
                LocalTime.of(5, 0), NOW, RunClock.LATE_START_GRACE));
    }

    @Test
    void waitUntilSleepsLongThenInMilliseconds() throws Exception {
        LocalDateTime[] now = {NOW};
        List<Double> sleeps = new ArrayList<>();

        RunClock.waitUntil(NOW.plusSeconds(62), () -> now[0], seconds -> {
            sleeps.add(seconds);
            now[0] = now[0].plusNanos(Math.round(seconds * 1e9));
        });

        assertEquals(30.0, sleeps.get(0));
        assertEquals(30.0, sleeps.get(1));
        assertEquals(1.0, sleeps.get(2), 1e-9);
        assertTrue(sleeps.subList(3, sleeps.size()).stream().allMatch(s -> s <= 0.001 + 1e-12));
        assertTrue(!now[0].isBefore(NOW.plusSeconds(62)));
        assertTrue(Duration.between(NOW.plusSeconds(62), now[0]).toMillis() <= 1);
    }
}
