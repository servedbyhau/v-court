package dev.vcourt.mobile;

import android.os.RemoteException;

import dev.vcourt.engine.driver.DriverException;
import dev.vcourt.engine.driver.DriverLauncher;
import dev.vcourt.engine.driver.DriverUnavailableException;
import dev.vcourt.engine.driver.LauncherState;
import rikka.shizuku.Shizuku;

/** Starts the driver through {@link ShellService}, bound through Shizuku for one session. */
final class ShizukuDriverLauncher implements DriverLauncher {
    private static final long BIND_TIMEOUT_MS = 10_000;

    private ShellBridge bridge;
    private int serviceUid = -1;

    @Override
    public void start() throws DriverException, InterruptedException {
        if (!Shizuku.pingBinder()) {
            throw new DriverUnavailableException("Shizuku is not running");
        }
        try {
            bridge = ShellBridge.bind(BIND_TIMEOUT_MS);
            serviceUid = bridge.state().uid;
            String error = bridge.startDriver();
            if (error != null) {
                throw new DriverUnavailableException("am instrument: " + error);
            }
        } catch (RemoteException | RuntimeException e) {
            throw new DriverUnavailableException("The Shizuku shell service failed: " + e, e);
        }
    }

    /** Return the UID the shell service runs as (2000 for shell, 0 for root), or -1. */
    int serviceUid() {
        return serviceUid;
    }

    @Override
    public LauncherState state() throws DriverException {
        try {
            ShellBridge.State state = connected().state();
            return new LauncherState(state.running,
                    state.exitCode == ShellService.NO_EXIT ? null : state.exitCode, state.output);
        } catch (RemoteException e) {
            throw new DriverException("The Shizuku shell service is gone: " + e.getMessage(), e);
        }
    }

    @Override
    public void kill() throws DriverException {
        try {
            connected().killDriver();
        } catch (RemoteException e) {
            throw new DriverException("The Shizuku shell service is gone: " + e.getMessage(), e);
        }
    }

    /**
     * Run a {@link ShellService} app command through this launcher's service.
     *
     * @throws DriverException when it failed or the service is gone
     */
    void appCommand(int op, String pkg) throws DriverException {
        try {
            String error = connected().appCommand(op, pkg);
            if (error != null) {
                throw new DriverException(error);
            }
        } catch (RemoteException e) {
            throw new DriverException("The Shizuku shell service is gone: " + e.getMessage(), e);
        }
    }

    @Override
    public void release() {
        if (bridge != null) {
            bridge.unbind();
            bridge = null;
        }
    }

    private ShellBridge connected() throws DriverException {
        if (bridge == null) {
            throw new DriverException("The driver was not started");
        }
        return bridge;
    }
}
