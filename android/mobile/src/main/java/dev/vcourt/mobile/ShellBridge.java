package dev.vcourt.mobile;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rikka.shizuku.Shizuku;

/** The app's side of {@link ShellService}: binds it through Shizuku and calls it. */
final class ShellBridge {
    /** Raised when the service changes; Shizuku then replaces a running older one. */
    private static final int SERVICE_VERSION = 2;

    /** What the service reports about the {@code am} process it started. */
    static final class State {
        final int uid;
        final boolean running;
        /** {@link ShellService#NO_EXIT} while {@code am} runs or before it ran. */
        final int exitCode;
        final String output;

        State(int uid, boolean running, int exitCode, String output) {
            this.uid = uid;
            this.running = running;
            this.exitCode = exitCode;
            this.output = output == null ? "" : output;
        }
    }

    private final Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
            new ComponentName(DriverContract.PACKAGE, ShellService.class.getName()))
            .daemon(false)
            .processNameSuffix("shell")
            .version(SERVICE_VERSION);
    private final CountDownLatch connected = new CountDownLatch(1);
    private volatile IBinder binder;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = service;
            connected.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder = null;
        }
    };

    /**
     * Bind the service and wait up to {@code timeoutMillis} for it; Shizuku must be running
     * and V Court authorized.
     *
     * @throws IllegalStateException when the service did not connect in time
     */
    static ShellBridge bind(long timeoutMillis) throws InterruptedException {
        ShellBridge bridge = new ShellBridge();
        Shizuku.bindUserService(bridge.args, bridge.connection);
        if (!bridge.connected.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
            bridge.unbind();
            throw new IllegalStateException(
                    "The Shizuku shell service did not connect within " + timeoutMillis + " ms");
        }
        return bridge;
    }

    /** Start the driver; return null when {@code am} started, else why it did not. */
    String startDriver() throws RemoteException {
        Parcel reply = call(ShellService.START_DRIVER);
        try {
            return reply.readString();
        } finally {
            reply.recycle();
        }
    }

    State state() throws RemoteException {
        Parcel reply = call(ShellService.DRIVER_STATE);
        try {
            return new State(reply.readInt(), reply.readInt() != 0, reply.readInt(),
                    reply.readString());
        } finally {
            reply.recycle();
        }
    }

    /** Run {@link ShellService}'s app command {@code op}; return null, or why it failed. */
    String appCommand(int op, String pkg) throws RemoteException {
        IBinder service = binder;
        if (service == null) {
            throw new RemoteException("The Shizuku shell service is not connected");
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(ShellService.DESCRIPTOR);
            data.writeInt(op);
            data.writeString(pkg);
            service.transact(ShellService.APP_COMMAND, data, reply, 0);
            reply.readException();
            return reply.readString();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    void killDriver() throws RemoteException {
        call(ShellService.KILL_DRIVER).recycle();
    }

    /** Unbind and end the service process. */
    void unbind() {
        try {
            Shizuku.unbindUserService(args, connection, true);
        } catch (RuntimeException ignored) {
            // Shizuku is gone, and the service with it: daemon(false)
        }
        binder = null;
    }

    private Parcel call(int code) throws RemoteException {
        IBinder service = binder;
        if (service == null) {
            throw new RemoteException("The Shizuku shell service is not connected");
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(ShellService.DESCRIPTOR);
            service.transact(code, data, reply, 0);
            reply.readException();
            return reply;
        } catch (RemoteException | RuntimeException e) {
            reply.recycle();
            throw e;
        } finally {
            data.recycle();
        }
    }
}
