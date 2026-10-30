package dev.vcourt.engine.driver;

/**
 * Starts and ends the process that runs the driver: on Android, {@code am instrument} run
 * through Shizuku. A launcher serves one {@link DriverSession}.
 */
public interface DriverLauncher {
    /**
     * Start the driver's process; return once it started, not once the driver serves.
     *
     * @throws DriverUnavailableException when the process cannot be started
     */
    void start() throws DriverException, InterruptedException;

    /**
     * Report on the process started.
     *
     * @throws DriverException when the launcher can no longer tell
     */
    LauncherState state() throws DriverException;

    /**
     * End the process at once: a last resort when the driver did not stop on request.
     *
     * @throws DriverException when the launcher can no longer reach it
     */
    void kill() throws DriverException;

    /** Free what the launcher holds; called once per session, whatever happened before. */
    void release();
}
