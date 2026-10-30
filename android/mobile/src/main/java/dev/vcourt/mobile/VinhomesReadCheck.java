package dev.vcourt.mobile;

import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import java.util.function.IntSupplier;

import dev.vcourt.engine.driver.DriverClient;
import dev.vcourt.engine.driver.DriverException;
import dev.vcourt.engine.driver.DriverSession;

/**
 * The F4 check, phase A's last gate: through the driver inside this APK, open Vinhomes
 * Resident, read its screen until the home screen shows, come back to V Court, tap V Court's
 * test target and stop the driver.
 *
 * <p>Nothing is tapped inside Vinhomes, and its screen may show personal data, so the report
 * holds only counts, timings and whether a home label showed; the dump is not logged or kept.
 */
final class VinhomesReadCheck implements Runnable {
    private static final String TAG = "VCourtVinhomes";
    /** Milliseconds Vinhomes may take to come to the front once started. */
    private static final long FRONT_TIMEOUT_MS = 15_000;
    /** Milliseconds its home screen may take to show after a cold start (splash, sign-in). */
    private static final long HOME_TIMEOUT_MS = 30_000;
    /** Milliseconds V Court may take to come back to the front. */
    private static final long RETURN_TIMEOUT_MS = 5_000;
    private static final long POLL_MS = 250;

    private final Context context;
    private final IntSupplier taps;
    private final CheckSupport.Listener listener;
    private DriverSession session;

    /** {@code taps} counts the taps V Court's test target received. */
    VinhomesReadCheck(Context context, IntSupplier taps, CheckSupport.Listener listener) {
        this.context = context.getApplicationContext();
        this.taps = taps;
        this.listener = listener;
    }

    @Override
    public void run() {
        report("V Court Vinhomes read check: app pid " + Process.myPid());
        try {
            check();
            report("RESULT: PASS");
        } catch (CheckSupport.Failure e) {
            report("FAIL " + e.getMessage());
            report("RESULT: FAIL");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            report("RESULT: FAIL (interrupted)");
        } catch (Exception e) {
            Log.e(TAG, "Check failed", e);
            report("FAIL " + e);
            report("RESULT: FAIL");
        } finally {
            if (session != null) {
                report("cleanup: " + CheckSupport.describe(session.stop()));
            }
        }
    }

    private void check() throws Exception {
        OneApkProof.checkShizuku(this::pass);
        Intent open = context.getPackageManager().getLaunchIntentForPackage(DriverContract.VINHOMES);
        if (open == null) {
            throw new CheckSupport.Failure("Vinhomes Resident (" + DriverContract.VINHOMES
                    + ") is not installed");
        }
        pass("Vinhomes Resident: installed");

        session = OneApkProof.openSession(context, new LocalSocketConnector(), this::pass);
        DriverClient client = session.client();

        long t = SystemClock.elapsedRealtime();
        context.startActivity(open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        if (!waitForFront(client, DriverContract.VINHOMES, FRONT_TIMEOUT_MS)) {
            throw new CheckSupport.Failure("Vinhomes: not in front after " + FRONT_TIMEOUT_MS
                    + " ms (" + client.currentPackage() + " is)");
        }
        pass("Vinhomes: in front after " + CheckSupport.since(t));

        readHome(client);

        t = SystemClock.elapsedRealtime();
        String how = comeBack(client);
        pass("V Court: back in front after " + CheckSupport.since(t) + " (" + how + ")");
        pass("V Court target: " + CheckSupport.tapTestTarget(client, taps));

        DriverSession.StopReport stopped = session.stop();
        session = null;
        if (!stopped.clean()) {
            throw new CheckSupport.Failure("stop: " + CheckSupport.describe(stopped));
        }
        pass("stop: " + CheckSupport.describe(stopped));
    }

    /**
     * Read Vinhomes until a home label shows; report each different read by its counts only.
     * A screen without labels is a splash: Flutter builds the labels later.
     */
    private void readHome(DriverClient client) throws Exception {
        long start = SystemClock.elapsedRealtime();
        long deadline = start + HOME_TIMEOUT_MS;
        int reads = 0;
        String last = null;
        while (true) {
            if (!DriverContract.VINHOMES.equals(client.currentPackage())) {
                throw new CheckSupport.Failure("Vinhomes: left the front while being read");
            }
            long t = SystemClock.elapsedRealtime();
            String xml = client.dump();
            long dumpTime = SystemClock.elapsedRealtime() - t;
            ScreenProbe screen = ScreenProbe.read(xml, DriverContract.HOME_LABELS);
            reads++;
            String line = xml.length() + " chars, " + screen.nodes + " nodes, " + screen.labelled
                    + " labelled";
            if (!line.equals(last)) {
                report("read " + reads + " at " + CheckSupport.since(start) + ": " + line + ", "
                        + dumpTime + " ms");
                last = line;
            }
            if (screen.found != null) {
                pass("Vinhomes home: \"" + screen.found + "\" shows at " + screen.bounds
                        + " after " + reads + " reads, " + CheckSupport.since(start));
                return;
            }
            if (SystemClock.elapsedRealtime() > deadline) {
                throw new CheckSupport.Failure("Vinhomes home: no " + DriverContract.HOME_LABELS
                        + " after " + HOME_TIMEOUT_MS + " ms; the screen may need a sign-in or"
                        + " show a notice: open Vinhomes by hand once, then run again");
            }
            Thread.sleep(POLL_MS);
        }
    }

    /**
     * Bring V Court back to the front: start its activity (the instrumented process may start
     * activities from the background), else press Back once, which leaves Vinhomes' home
     * screen. Nothing inside Vinhomes is tapped.
     */
    private String comeBack(DriverClient client) throws Exception {
        Intent back = new Intent(context, DriverChecksActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(back);
        if (waitForFront(client, DriverContract.PACKAGE, RETURN_TIMEOUT_MS)) {
            return "activity start";
        }
        client.back();
        if (waitForFront(client, DriverContract.PACKAGE, RETURN_TIMEOUT_MS)) {
            return "Back, the activity start did not bring it";
        }
        throw new CheckSupport.Failure("V Court: not back in front (" + client.currentPackage()
                + " is); open V Court by hand");
    }

    private static boolean waitForFront(DriverClient client, String pkg, long timeoutMillis)
            throws DriverException, InterruptedException {
        long deadline = SystemClock.elapsedRealtime() + timeoutMillis;
        while (!pkg.equals(client.currentPackage())) {
            if (SystemClock.elapsedRealtime() > deadline) {
                return false;
            }
            Thread.sleep(POLL_MS);
        }
        return true;
    }

    private void pass(String line) {
        report("PASS " + line);
    }

    private void report(String line) {
        Log.i(TAG, line);
        listener.report(line);
    }
}
