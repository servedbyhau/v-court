package dev.vcourt.mobile;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.function.IntSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.vcourt.engine.driver.ClickResult;
import dev.vcourt.engine.driver.DriverClient;
import dev.vcourt.engine.driver.DriverInUseException;
import dev.vcourt.engine.driver.DriverSession;
import dev.vcourt.engine.driver.LauncherState;
import rikka.shizuku.Shizuku;

/**
 * The F2a proof: open a {@link DriverSession} on the driver inside this APK, read V Court's
 * own screen, tap V Court's test target, stop the session and check the driver is gone.
 *
 * <p>Each step reports one line; the report holds no personal data (it reads only V Court's
 * screen), so it can be pasted as evidence. The session is stopped whatever step failed.
 */
final class OneApkProof implements Runnable {
    /** Receives each line of the report, on the proof's thread. */
    interface Listener {
        void report(String line);
    }

    private static final String TAG = "VCourtProof";
    private static final long TAP_TIMEOUT_MS = 2_000;
    private static final long POLL_MS = 100;
    private static final Pattern BOUNDS = Pattern.compile("\\[(-?\\d+),(-?\\d+)]\\[(-?\\d+),(-?\\d+)]");

    /** A step that did not give what the proof needs; its message says which and why. */
    private static final class Failure extends Exception {
        private static final long serialVersionUID = 1L;

        Failure(String message) {
            super(message);
        }
    }

    private final Context context;
    private final IntSupplier taps;
    private final Listener listener;
    private final LocalSocketConnector connector = new LocalSocketConnector();
    private DriverSession session;

    /** {@code taps} counts the taps V Court's test target received. */
    OneApkProof(Context context, IntSupplier taps, Listener listener) {
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
        } catch (Failure e) {
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
                DriverSession.StopReport stopped = session.stop();
                report("cleanup: " + describe(stopped));
            }
        }
    }

    private void prove() throws Exception {
        if (!Shizuku.pingBinder()) {
            throw new Failure("Shizuku: not running");
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            throw new Failure("Shizuku: V Court is not authorized");
        }
        pass("Shizuku: running as uid " + Shizuku.getUid() + ", V Court authorized");

        InstrumentationInfo info = context.getPackageManager().getInstrumentationInfo(
                new ComponentName(DriverContract.PACKAGE, DriverContract.INSTRUMENTATION), 0);
        if (!DriverContract.PACKAGE.equals(info.targetPackage)) {
            throw new Failure("Instrumentation targets " + info.targetPackage);
        }
        pass("Instrumentation: declared in this APK, targets " + info.targetPackage);

        ShizukuDriverLauncher launcher = new ShizukuDriverLauncher();
        long t = SystemClock.elapsedRealtime();
        try {
            session = DriverSession.open(launcher, connector, ownVersion());
        } catch (DriverInUseException e) {
            throw new Failure("Socket: " + e.getMessage() + "; stop the other driver first");
        }
        pass("Driver: started through a shell service (uid " + launcher.serviceUid()
                + ") and greeted in " + since(t) + ": " + session.hello());
        DriverClient client = session.client();

        t = SystemClock.elapsedRealtime();
        String current = client.currentPackage();
        if (!DriverContract.PACKAGE.equals(current)) {
            throw new Failure("current: active window is " + current + ", not V Court");
        }
        pass("current: " + current + ", " + since(t));

        t = SystemClock.elapsedRealtime();
        String xml = client.dump();
        long dumpTime = SystemClock.elapsedRealtime() - t;
        Target target = find(xml, DriverContract.TEST_TARGET);
        if (target.bounds == null) {
            throw new Failure("dump: " + target.nodes + " nodes, no \"" + DriverContract.TEST_TARGET
                    + "\"");
        }
        pass("dump: " + xml.length() + " chars, " + target.nodes + " nodes, target at "
                + target.bounds + ", " + dumpTime + " ms");

        int before = taps.getAsInt();
        t = SystemClock.elapsedRealtime();
        ClickResult click = client.click(DriverContract.TEST_TARGET, target.x, target.y);
        if (!click.clicked()) {
            throw new Failure("click: the driver found no target");
        }
        long clickTime = SystemClock.elapsedRealtime() - t;
        if (!tapArrived(before)) {
            throw new Failure("click: sent (" + click.how() + ") but the target saw no tap");
        }
        pass("click: " + click.how() + " on " + click.bounds() + ", target saw it, "
                + clickTime + " ms");

        DriverSession.StopReport stopped = session.stop();
        if (!stopped.clean()) {
            throw new Failure("stop: " + describe(stopped));
        }
        pass("stop: " + describe(stopped));
        session = null;
        try {
            connector.connect().close();
            throw new Failure("stop: " + DriverContract.SOCKET + " still answers");
        } catch (IOException expected) {
            pass("stop: " + DriverContract.SOCKET + " closed; app pid still " + Process.myPid());
        }
    }

    private String ownVersion() throws PackageManager.NameNotFoundException {
        return context.getPackageManager().getPackageInfo(DriverContract.PACKAGE, 0).versionName;
    }

    private boolean tapArrived(int before) throws InterruptedException {
        long deadline = SystemClock.elapsedRealtime() + TAP_TIMEOUT_MS;
        while (taps.getAsInt() <= before) {
            if (SystemClock.elapsedRealtime() > deadline) {
                return false;
            }
            Thread.sleep(POLL_MS);
        }
        return true;
    }

    /** Return how a session ended, on one line. */
    static String describe(DriverSession.StopReport stopped) {
        LauncherState state = stopped.state();
        StringBuilder line = new StringBuilder(stopped.asked() ? "driver stopped" : "stop not delivered");
        if (state != null) {
            line.append(state.running() ? ", am still runs" : ", am ended with " + state.exitCode())
                    .append(" (").append(lastLine(state.output())).append(")");
        }
        if (stopped.killed()) {
            line.append(", am killed: restart V Court before the next run");
        }
        if (stopped.problem() != null) {
            line.append(", ").append(stopped.problem());
        }
        return line.toString();
    }

    /** One node count and the test target's place in a dump. */
    private static final class Target {
        int nodes;
        String bounds;
        int x;
        int y;
    }

    private static Target find(String xml, String label) throws XmlPullParserException,
            IOException {
        Target target = new Target();
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new StringReader(xml));
        for (int event = parser.getEventType(); event != XmlPullParser.END_DOCUMENT;
                event = parser.next()) {
            if (event != XmlPullParser.START_TAG || !"node".equals(parser.getName())) {
                continue;
            }
            target.nodes++;
            if (target.bounds == null
                    && label.equals(parser.getAttributeValue(null, "content-desc"))) {
                Matcher m = BOUNDS.matcher(parser.getAttributeValue(null, "bounds"));
                if (m.matches()) {
                    target.bounds = m.group();
                    target.x = (Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(3))) / 2;
                    target.y = (Integer.parseInt(m.group(2)) + Integer.parseInt(m.group(4))) / 2;
                }
            }
        }
        return target;
    }

    private static String since(long start) {
        return (SystemClock.elapsedRealtime() - start) + " ms";
    }

    private static String lastLine(String output) {
        String trimmed = output.trim();
        return trimmed.isEmpty() ? "no output" : trimmed.substring(trimmed.lastIndexOf('\n') + 1);
    }

    private void pass(String line) {
        report("PASS " + line);
    }

    private void report(String line) {
        Log.i(TAG, line);
        listener.report(line);
    }
}
