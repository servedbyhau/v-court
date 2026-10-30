package dev.vcourt.mobile;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.util.function.IntSupplier;

import dev.vcourt.engine.driver.DriverInUseException;
import dev.vcourt.engine.driver.DriverSession;
import rikka.shizuku.Shizuku;

/**
 * The F2a proof: open a {@link DriverSession} on the driver inside this APK, read V Court's
 * own screen, tap V Court's test target, stop the session and check the driver is gone.
 *
 * <p>Each step reports one line; the report holds no personal data (it reads only V Court's
 * screen), so it can be pasted as evidence. The session is stopped whatever step failed.
 */
final class OneApkProof implements Runnable {
    private static final String TAG = "VCourtProof";

    private final Context context;
    private final IntSupplier taps;
    private final CheckSupport.Listener listener;
    private final LocalSocketConnector connector = new LocalSocketConnector();
    private DriverSession session;

    /** {@code taps} counts the taps V Court's test target received. */
    OneApkProof(Context context, IntSupplier taps, CheckSupport.Listener listener) {
        this.context = context.getApplicationContext();
        this.taps = taps;
        this.listener = listener;
    }

    @Override
    public void run() {
        report("V Court one-APK proof: app pid " + Process.myPid() + ", driver "
                + DriverContract.COMPONENT);
        try {
            prove();
            report("RESULT: PASS");
        } catch (CheckSupport.Failure e) {
            report("FAIL " + e.getMessage());
            report("RESULT: FAIL");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            report("RESULT: FAIL (interrupted)");
        } catch (Exception e) {
            Log.e(TAG, "Proof failed", e);
            report("FAIL " + e);
            report("RESULT: FAIL");
        } finally {
            if (session != null) {
                report("cleanup: " + CheckSupport.describe(session.stop()));
            }
        }
    }

    private void prove() throws Exception {
        checkShizuku(this::pass);
        InstrumentationInfo info = context.getPackageManager().getInstrumentationInfo(
                new ComponentName(DriverContract.PACKAGE, DriverContract.INSTRUMENTATION), 0);
        if (!DriverContract.PACKAGE.equals(info.targetPackage)) {
            throw new CheckSupport.Failure("Instrumentation targets " + info.targetPackage);
        }
        pass("Instrumentation: declared in this APK, targets " + info.targetPackage);

        session = openSession(context, connector, this::pass);

        long t = SystemClock.elapsedRealtime();
        String current = session.client().currentPackage();
        if (!DriverContract.PACKAGE.equals(current)) {
            throw new CheckSupport.Failure("current: active window is " + current
                    + ", not V Court");
        }
        pass("current: " + current + ", " + CheckSupport.since(t));
        pass("V Court target: " + CheckSupport.tapTestTarget(session.client(), taps));

        DriverSession.StopReport stopped = session.stop();
        if (!stopped.clean()) {
            throw new CheckSupport.Failure("stop: " + CheckSupport.describe(stopped));
        }
        pass("stop: " + CheckSupport.describe(stopped));
        session = null;
        try {
            connector.connect().close();
            throw new CheckSupport.Failure("stop: " + DriverContract.SOCKET + " still answers");
        } catch (IOException expected) {
            pass("stop: " + DriverContract.SOCKET + " closed; app pid still " + Process.myPid());
        }
    }

    /** Fail unless Shizuku runs and V Court may use it; report it to {@code pass} otherwise. */
    static void checkShizuku(CheckSupport.Listener pass) throws CheckSupport.Failure {
        if (!Shizuku.pingBinder()) {
            throw new CheckSupport.Failure("Shizuku: not running");
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            throw new CheckSupport.Failure("Shizuku: V Court is not authorized");
        }
        pass.report("Shizuku: running as uid " + Shizuku.getUid() + ", V Court authorized");
    }

    /** Start and greet the driver inside this APK; report how long it took to {@code pass}. */
    static DriverSession openSession(Context context, LocalSocketConnector connector,
            CheckSupport.Listener pass) throws Exception {
        String version = context.getPackageManager()
                .getPackageInfo(DriverContract.PACKAGE, 0).versionName;
        ShizukuDriverLauncher launcher = new ShizukuDriverLauncher();
        long t = SystemClock.elapsedRealtime();
        DriverSession session;
        try {
            session = DriverSession.open(launcher, connector, version);
        } catch (DriverInUseException e) {
            throw new CheckSupport.Failure("Socket: " + e.getMessage()
                    + "; stop the other driver first");
        }
        pass.report("Driver: started through a shell service (uid " + launcher.serviceUid()
                + ") and greeted in " + CheckSupport.since(t) + ": " + session.hello());
        return session;
    }

    private void pass(String line) {
        report("PASS " + line);
    }

    private void report(String line) {
        Log.i(TAG, line);
        listener.report(line);
    }
}
