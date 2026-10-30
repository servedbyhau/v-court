package dev.vcourt.engine.driver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class DriverSessionTest {
    private static final String VERSION = "0.1.0-dev";

    private final FakeLauncher launcher = new FakeLauncher().runsFor(FakeLauncher.FOREVER);
    private final FakeConnector connector = new FakeConnector();
    private final FakeClock clock = new FakeClock();

    private static String hello(long id, int protocol, String version) {
        return "{\"id\":" + id + ",\"ok\":true,\"protocol\":" + protocol + ",\"version\":\""
                + version + "\",\"sdk\":35}";
    }

    private static FakeTransport greeting() {
        return new FakeTransport().then(hello(1, 1, VERSION));
    }

    private DriverSession open() throws Exception {
        return DriverSession.open(launcher, connector, VERSION, clock, 250);
    }

    @Test
    void refusesADriverItDidNotStartAndLeavesItAlone() {
        FakeTransport other = new FakeTransport();
        connector.then(other);

        assertThrows(DriverInUseException.class, this::open);

        assertEquals(0, launcher.started);
        assertEquals(Collections.emptyList(), other.sent);
        assertTrue(other.closed);
        assertEquals(1, launcher.released);
    }

    @Test
    void startsTheDriverAndGreetsItOnceItServes() throws Exception {
        FakeTransport transport = greeting();
        connector.then(FakeConnector.FREE).then(FakeConnector.FREE).then(FakeConnector.FREE)
                .then(transport);

        DriverSession session = open();

        assertEquals(1, launcher.started);
        assertEquals(VERSION, session.hello().version());
        assertEquals(35, session.hello().sdk());
        assertEquals(List.of(100L, 100L), clock.sleeps);
        assertEquals(List.of("{\"id\":1,\"op\":\"hello\"}"), transport.sent);
        assertEquals(0, launcher.released);
    }

    @Test
    void greetsAgainWhenTheFirstConnectionDrops() throws Exception {
        FakeTransport dropped = new FakeTransport().then(FakeTransport.EOF);
        connector.then(FakeConnector.FREE).then(dropped).then(greeting());

        DriverSession session = open();

        assertTrue(dropped.closed);
        assertEquals(VERSION, session.hello().version());
        assertEquals(List.of(100L), clock.sleeps);
    }

    @Test
    void reportsWhatTheProcessPrintedWhenItEndsBeforeAnswering() {
        launcher.runsFor(0);
        launcher.exitCode = 1;
        launcher.output = "Error: Unknown option: --no-restart";

        DriverUnavailableException e = assertThrows(DriverUnavailableException.class,
                this::open);

        assertEquals("The driver ended before answering (exit 1): "
                + "Error: Unknown option: --no-restart", e.getMessage());
        assertEquals(0, launcher.killed);
        assertEquals(1, launcher.released);
    }

    @Test
    void endsTheProcessWhenTheDriverNeverAnswers() {
        DriverUnavailableException e = assertThrows(DriverUnavailableException.class,
                this::open);

        assertTrue(e.getMessage().startsWith("The driver did not answer 10 s after being started"),
                e.getMessage());
        assertEquals(1, launcher.killed);
        assertEquals(1, launcher.released);
        assertEquals(DriverSession.START_TIMEOUT_MILLIS + DriverSession.STOP_TIMEOUT_MILLIS,
                clock.sleeps.stream().mapToLong(Long::longValue).sum());
    }

    @Test
    void refusesAnotherProtocolAndNeverStopsThatDriver() {
        FakeTransport first = new FakeTransport().then(hello(1, 2, VERSION));
        FakeTransport second = new FakeTransport().then(hello(1, 2, VERSION));
        connector.then(FakeConnector.FREE).then(first).then(second);

        DriverUnavailableException e = assertThrows(DriverUnavailableException.class,
                this::open);

        assertEquals("The driver 0.1.0-dev speaks protocol 2, not 1", e.getMessage());
        assertFalse(first.sent.contains("{\"id\":2,\"op\":\"stop\"}"));
        assertEquals(List.of("{\"id\":1,\"op\":\"hello\"}"), second.sent);
        assertEquals(1, launcher.killed);
        assertEquals(1, launcher.released);
    }

    @Test
    void refusesAnotherVersionAndNeverStopsThatDriver() {
        FakeTransport first = new FakeTransport().then(hello(1, 1, "4f2c9a1b7e3d"));
        FakeTransport second = new FakeTransport().then(hello(1, 1, "4f2c9a1b7e3d"));
        connector.then(FakeConnector.FREE).then(first).then(second);

        DriverUnavailableException e = assertThrows(DriverUnavailableException.class,
                this::open);

        assertEquals("The driver answering is 4f2c9a1b7e3d, not this app's 0.1.0-dev",
                e.getMessage());
        assertEquals(List.of("{\"id\":1,\"op\":\"hello\"}"), first.sent);
        assertEquals(List.of("{\"id\":1,\"op\":\"hello\"}"), second.sent);
        assertEquals(1, launcher.killed);
    }

    @Test
    void aStartThatFailsOnlyReleasesTheLauncher() {
        launcher.startFailure = new DriverUnavailableException("am did not start: denied");

        DriverUnavailableException e = assertThrows(DriverUnavailableException.class,
                this::open);

        assertEquals("am did not start: denied", e.getMessage());
        assertEquals(0, launcher.stateCalls);
        assertEquals(1, launcher.released);
    }

    @Test
    void anInterruptedStartStopsWhatItStarted() {
        clock.interruptAtSleep = 0;

        assertThrows(InterruptedException.class, this::open);

        assertEquals(1, launcher.killed);
        assertEquals(1, launcher.released);
        assertFalse(Thread.currentThread().isInterrupted());
    }

    @Test
    void stopAsksTheDriverAndWaitsForItsProcess() throws Exception {
        FakeTransport transport = greeting().then("{\"id\":2,\"ok\":true}");
        connector.then(FakeConnector.FREE).then(transport);
        DriverSession session = open();
        launcher.runsFor(2);

        DriverSession.StopReport report = session.stop();

        assertEquals("{\"id\":2,\"op\":\"stop\"}", transport.sent.get(1));
        assertTrue(report.asked());
        assertFalse(report.killed());
        assertTrue(report.clean());
        assertEquals(-1, report.state().exitCode());
        assertEquals(List.of(100L, 100L), clock.sleeps);
        assertEquals(1, launcher.released);
        assertTrue(transport.closed);
    }

    @Test
    void stopIsDoneOnce() throws Exception {
        FakeTransport transport = greeting().then("{\"id\":2,\"ok\":true}");
        connector.then(FakeConnector.FREE).then(transport);
        DriverSession session = open();
        launcher.runsFor(0);

        DriverSession.StopReport first = session.stop();
        session.close();

        assertSame(first, session.stop());
        assertEquals(2, transport.sent.size());
        assertEquals(1, launcher.released);
    }

    @Test
    void stopEndsAProcessThatOutlivesTheStop() throws Exception {
        connector.then(FakeConnector.FREE).then(greeting().then("{\"id\":2,\"ok\":true}"));
        DriverSession session = open();

        DriverSession.StopReport report = session.stop();

        assertTrue(report.asked());
        assertTrue(report.killed());
        assertFalse(report.clean());
        assertEquals(143, report.state().exitCode());
        assertEquals(1, launcher.killed);
        assertEquals(DriverSession.STOP_TIMEOUT_MILLIS,
                clock.sleeps.stream().mapToLong(Long::longValue).sum());
    }

    @Test
    void cancelFailsTheNextCommandAndStopAsksOverANewConnection() throws Exception {
        FakeTransport first = greeting();
        FakeTransport second = new FakeTransport().then(hello(1, 1, VERSION))
                .then("{\"id\":2,\"ok\":true}");
        connector.then(FakeConnector.FREE).then(first).then(second);
        DriverSession session = open();

        session.cancel();

        assertTrue(first.closed);
        assertThrows(DriverException.class, () -> session.client().dump());
        launcher.runsFor(0);
        DriverSession.StopReport report = session.stop();
        assertTrue(report.asked());
        assertEquals(List.of("{\"id\":1,\"op\":\"hello\"}", "{\"id\":2,\"op\":\"stop\"}"),
                second.sent);
        assertTrue(report.clean());
    }

    @Test
    void aStopThatCannotReachTheDriverStillEndsTheProcess() throws Exception {
        FakeTransport transport = greeting().then(FakeTransport.EOF);
        connector.then(FakeConnector.FREE).then(transport);
        DriverSession session = open();

        DriverSession.StopReport report = session.stop();

        assertFalse(report.asked());
        assertTrue(report.killed());
        assertEquals(1, launcher.released);
    }
}
