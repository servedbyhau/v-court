package dev.vcourt.engine.driver;

/** A launcher whose process runs for a set number of looks, counting what was asked. */
final class FakeLauncher implements DriverLauncher {
    static final int FOREVER = Integer.MAX_VALUE;

    int started;
    int killed;
    int released;
    int stateCalls;
    DriverException startFailure;
    Integer exitCode = -1;
    String output = "INSTRUMENTATION_CODE: -1";
    private int runningLooks;

    /** Report running for the next {@code looks} calls to {@link #state}, then ended. */
    FakeLauncher runsFor(int looks) {
        runningLooks = looks;
        return this;
    }

    @Override
    public void start() throws DriverException {
        if (startFailure != null) {
            throw startFailure;
        }
        started++;
    }

    @Override
    public LauncherState state() {
        stateCalls++;
        if (started > 0 && runningLooks > 0) {
            if (runningLooks != FOREVER) {
                runningLooks--;
            }
            return new LauncherState(true, null, "");
        }
        return new LauncherState(false, started > 0 ? exitCode : null, output);
    }

    @Override
    public void kill() {
        killed++;
        runningLooks = 0;
        exitCode = 143;
    }

    @Override
    public void release() {
        released++;
    }
}
