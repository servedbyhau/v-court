package dev.vcourt.engine.driver;

import java.io.IOException;

/**
 * One driver that this app started, greeted and will stop: open it, use {@link #client()},
 * close it. Ported from the desktop's {@code connect_driver} and {@code stop_driver}
 * ({@code court_booker.driver}), with V Court's ownership rule added.
 *
 * <p>Opening refuses a driver it did not start ({@link DriverInUseException}): whatever
 * already serves the socket belongs to someone else and is never taken over or stopped. It
 * then starts the driver, connects every {@link #POLL_MILLIS} and greets it until
 * {@link #START_TIMEOUT_MILLIS}: the driver must speak {@link DriverClient#PROTOCOL} and be
 * this app's version. A failure after the start stops what was started.
 *
 * <p>Stopping asks the driver to stop, waits {@link #STOP_TIMEOUT_MILLIS} for its process to
 * end and ends it otherwise. {@link #cancel} may be called from any thread: it breaks the
 * connection at once, so a command in progress fails; the owner then closes the session,
 * which asks for the stop over a new connection.
 */
public final class DriverSession implements AutoCloseable {
    /** Milliseconds the driver may take to serve once started; it took 0.8-2.5 s on MuMu. */
    public static final long START_TIMEOUT_MILLIS = 10_000;
    /** Milliseconds the driver's process may take to end after {@code stop}. */
    public static final long STOP_TIMEOUT_MILLIS = 5_000;
    /** Milliseconds between two tries to connect, and between two looks at the process. */
    public static final long POLL_MILLIS = 100;

    /** Monotonic time and sleeping; tests replace them. */
    public interface Clock {
        long millis();

        void sleep(long millis) throws InterruptedException;
    }

    /** The system's monotonic clock. */
    public static final Clock SYSTEM_CLOCK = new Clock() {
        @Override
        public long millis() {
            return System.nanoTime() / 1_000_000;
        }

        @Override
        public void sleep(long millis) throws InterruptedException {
            Thread.sleep(millis);
        }
    };

    /** How a session ended. */
    public static final class StopReport {
        private final boolean asked;
        private final boolean killed;
        private final LauncherState state;
        private final String problem;

        StopReport(boolean asked, boolean killed, LauncherState state, String problem) {
            this.asked = asked;
            this.killed = killed;
            this.state = state;
            this.problem = problem;
        }

        /** Tell whether the driver accepted {@code stop}. */
        public boolean asked() {
            return asked;
        }

        /**
         * Tell whether the process had to be ended. The driver may then still hold its
         * socket in the app's process, until the app restarts.
         */
        public boolean killed() {
            return killed;
        }

        /** Return the process's last known state, or {@code null} when the launcher failed. */
        public LauncherState state() {
            return state;
        }

        /** Return what went wrong while stopping, or {@code null}. */
        public String problem() {
            return problem;
        }

        /** Tell whether the driver stopped on request and its process ended by itself. */
        public boolean clean() {
            return asked && !killed && problem == null && state != null && !state.running();
        }
    }

    private final DriverLauncher launcher;
    private final DriverConnector connector;
    private final String version;
    private final Clock clock;
    private final long commandTimeoutMillis;
    private final DriverTransport transport;
    private final DriverClient client;
    private final Hello hello;
    private final Object stopLock = new Object();
    private StopReport report;

    private DriverSession(DriverLauncher launcher, DriverConnector connector, String version,
            Clock clock, long commandTimeoutMillis, DriverTransport transport,
            DriverClient client, Hello hello) {
        this.launcher = launcher;
        this.connector = connector;
        this.version = version;
        this.clock = clock;
        this.commandTimeoutMillis = commandTimeoutMillis;
        this.transport = transport;
        this.client = client;
        this.hello = hello;
    }

    /**
     * Start a driver of {@code version} with {@code launcher} and connect to it.
     *
     * @throws DriverInUseException when something already serves the socket
     * @throws DriverUnavailableException when the driver does not start, answer or match
     * @throws InterruptedException when the thread is interrupted; what started is stopped
     */
    public static DriverSession open(DriverLauncher launcher, DriverConnector connector,
            String version) throws DriverException, InterruptedException {
        return open(launcher, connector, version, SYSTEM_CLOCK,
                DriverClient.COMMAND_TIMEOUT_MILLIS);
    }

    /** As {@link #open(DriverLauncher, DriverConnector, String)}, with a clock and timeout. */
    public static DriverSession open(DriverLauncher launcher, DriverConnector connector,
            String version, Clock clock, long commandTimeoutMillis)
            throws DriverException, InterruptedException {
        DriverTransport other = tryConnect(connector);
        if (other != null) {
            other.close();
            launcher.release();
            throw new DriverInUseException("Another driver already serves the socket;"
                    + " V Court does not take it over");
        }
        try {
            launcher.start();
        } catch (DriverException | InterruptedException | RuntimeException e) {
            launcher.release();
            throw e;
        }
        try {
            return await(launcher, connector, version, clock, commandTimeoutMillis);
        } catch (DriverException | InterruptedException | RuntimeException e) {
            boolean interrupted = Thread.interrupted();
            awaitEnd(launcher, clock, askToStop(connector, version, commandTimeoutMillis));
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            throw e;
        }
    }

    private static DriverSession await(DriverLauncher launcher, DriverConnector connector,
            String version, Clock clock, long commandTimeoutMillis)
            throws DriverException, InterruptedException {
        long deadline = clock.millis() + START_TIMEOUT_MILLIS;
        while (true) {
            DriverTransport transport = tryConnect(connector);
            if (transport != null) {
                DriverClient client = new DriverClient(transport, commandTimeoutMillis);
                Hello hello = greet(client);
                if (hello != null) {
                    if (!hello.version().equals(version)) {
                        client.close();
                        throw new DriverUnavailableException("The driver answering is "
                                + hello.version() + ", not this app's " + version);
                    }
                    return new DriverSession(launcher, connector, version, clock,
                            commandTimeoutMillis, transport, client, hello);
                }
            }
            LauncherState state = launcher.state();
            if (!state.running()) {
                throw new DriverUnavailableException("The driver ended before answering (exit "
                        + state.exitCode() + "): " + state.outputLine());
            }
            if (clock.millis() >= deadline) {
                throw new DriverUnavailableException("The driver did not answer "
                        + START_TIMEOUT_MILLIS / 1000 + " s after being started: "
                        + state.outputLine());
            }
            clock.sleep(POLL_MILLIS);
        }
    }

    /**
     * Return the driver's hello, or {@code null} when the connection broke (a driver still
     * starting, as the desktop retries). A refusal or another protocol leaves the
     * connection whole and is final.
     */
    private static Hello greet(DriverClient client) throws DriverUnavailableException {
        try {
            return client.hello();
        } catch (DriverException e) {
            boolean broken = client.isBroken();
            client.close();
            if (broken) {
                return null;
            }
            throw new DriverUnavailableException(e.getMessage(), e);
        }
    }

    /** Return the client that carries this session's commands. */
    public DriverClient client() {
        return client;
    }

    /** Return the driver's answer to the greeting. */
    public Hello hello() {
        return hello;
    }

    /**
     * Break the connection at once, from any thread: a command in progress fails. Close the
     * session afterwards to stop the driver.
     */
    public void cancel() {
        transport.close();
    }

    /**
     * Stop the driver and free the launcher; a later call returns the same report. Never
     * throws: a session ends whatever failed.
     */
    public StopReport stop() {
        synchronized (stopLock) {
            if (report == null) {
                boolean asked = false;
                if (!client.isBroken()) {
                    try {
                        client.stop();
                        asked = true;
                    } catch (DriverException e) {
                        // asked again over a new connection below
                    }
                }
                client.close();
                if (!asked) {
                    asked = askToStop(connector, version, commandTimeoutMillis);
                }
                report = awaitEnd(launcher, clock, asked);
            }
            return report;
        }
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * Ask the driver behind a new connection to stop, only if it is this app's version: one
     * that is not was not started here.
     */
    private static boolean askToStop(DriverConnector connector, String version,
            long commandTimeoutMillis) {
        DriverTransport transport = tryConnect(connector);
        if (transport == null) {
            return false;
        }
        try (DriverClient client = new DriverClient(transport, commandTimeoutMillis)) {
            if (!client.hello().version().equals(version)) {
                return false;
            }
            client.stop();
            return true;
        } catch (DriverException e) {
            return false;
        }
    }

    /** Wait for the process to end, end it otherwise, and free the launcher. */
    private static StopReport awaitEnd(DriverLauncher launcher, Clock clock, boolean asked) {
        boolean interrupted = false;
        boolean killed = false;
        LauncherState state = null;
        String problem = null;
        try {
            long deadline = clock.millis() + STOP_TIMEOUT_MILLIS;
            state = launcher.state();
            while (state.running() && !interrupted && clock.millis() < deadline) {
                try {
                    clock.sleep(POLL_MILLIS);
                } catch (InterruptedException e) {
                    interrupted = true;
                }
                state = launcher.state();
            }
            if (state.running()) {
                launcher.kill();
                killed = true;
                state = launcher.state();
            }
        } catch (DriverException | RuntimeException e) {
            problem = e.getMessage();
        } finally {
            launcher.release();
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return new StopReport(asked, killed, state, problem);
    }

    private static DriverTransport tryConnect(DriverConnector connector) {
        try {
            return connector.connect();
        } catch (IOException e) {
            return null;
        }
    }
}
