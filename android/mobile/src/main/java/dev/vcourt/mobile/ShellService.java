package dev.vcourt.mobile;

import android.os.Binder;
import android.os.Parcel;
import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;

import rikka.shizuku.ShizukuApiConstants;

/**
 * The Shizuku user service: runs in its own process as the shell user and starts the driver
 * with {@code am instrument -w --no-restart}, which only the shell may do.
 *
 * <p>{@code --no-restart} loads the driver into the running V Court process; without it
 * Android would force-stop V Court first. The {@code am} process must keep running: it holds
 * the driver's UiAutomation connection. The service runs that one command and nothing else.
 *
 * <p>A hand-written binder, so the build needs no AIDL compiler; {@link ShellBridge} is its
 * client.
 */
public final class ShellService extends Binder {
    static final String DESCRIPTOR = "dev.vcourt.mobile.ShellService";
    /** Start the driver; replies an error message, or null when {@code am} started. */
    static final int START_DRIVER = FIRST_CALL_TRANSACTION;
    /** Reply the service's UID, whether {@code am} runs, its exit code and its output. */
    static final int DRIVER_STATE = FIRST_CALL_TRANSACTION + 1;
    /** End {@code am}: a last resort when the driver did not stop on request. */
    static final int KILL_DRIVER = FIRST_CALL_TRANSACTION + 2;
    /** Exit code reported while {@code am} runs or before it ever ran. */
    static final int NO_EXIT = Integer.MIN_VALUE;

    private static final String TAG = "VCourtShell";
    private static final int OUTPUT_LINES = 30;
    private static final int LINE_LENGTH = 300;

    private final ArrayDeque<String> output = new ArrayDeque<>();
    private java.lang.Process am;
    private int exitCode = NO_EXIT;

    /** Shizuku creates the service with this constructor. */
    public ShellService() {
        attachInterface(null, DESCRIPTOR);
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws android.os.RemoteException {
        switch (code) {
            case ShizukuApiConstants.USER_SERVICE_TRANSACTION_destroy:
                kill();
                System.exit(0);
                return true;
            case START_DRIVER: {
                data.enforceInterface(DESCRIPTOR);
                String error = start();
                reply.writeNoException();
                reply.writeString(error);
                return true;
            }
            case DRIVER_STATE:
                data.enforceInterface(DESCRIPTOR);
                reply.writeNoException();
                writeState(reply);
                return true;
            case KILL_DRIVER:
                data.enforceInterface(DESCRIPTOR);
                kill();
                reply.writeNoException();
                return true;
            default:
                return super.onTransact(code, data, reply, flags);
        }
    }

    private synchronized String start() {
        if (running()) {
            return "The driver this service started still runs";
        }
        output.clear();
        exitCode = NO_EXIT;
        java.lang.Process started;
        try {
            started = new ProcessBuilder("/system/bin/am", "instrument", "-w", "--no-restart",
                    DriverContract.COMPONENT).redirectErrorStream(true).start();
        } catch (IOException e) {
            return "am did not start: " + e.getMessage();
        }
        am = started;
        Thread reader = new Thread(() -> follow(started), "am-output");
        reader.setDaemon(true);
        reader.start();
        Log.i(TAG, "Started " + DriverContract.COMPONENT);
        return null;
    }

    /** Keep the last lines {@code am} prints, then its exit code once it ends. */
    private void follow(java.lang.Process process) {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                keep(process, line);
            }
        } catch (IOException e) {
            keep(process, "(output ended: " + e.getMessage() + ")");
        }
        int code;
        try {
            code = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        synchronized (this) {
            if (am == process) {
                exitCode = code;
            }
        }
        Log.i(TAG, "am ended with " + code);
    }

    private synchronized void keep(java.lang.Process process, String line) {
        if (am != process) {
            return;
        }
        output.addLast(line.length() > LINE_LENGTH ? line.substring(0, LINE_LENGTH) : line);
        while (output.size() > OUTPUT_LINES) {
            output.removeFirst();
        }
    }

    private synchronized boolean running() {
        return am != null && exitCode == NO_EXIT && am.isAlive();
    }

    private synchronized void writeState(Parcel reply) {
        reply.writeInt(Process.myUid());
        reply.writeInt(running() ? 1 : 0);
        reply.writeInt(exitCode);
        reply.writeString(String.join("\n", output));
    }

    private synchronized void kill() {
        if (running()) {
            am.destroy();
            Log.w(TAG, "Ended am");
        }
    }
}
