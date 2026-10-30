package dev.vcourt.mobile;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageManager;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
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
import dev.vcourt.engine.driver.DriverException;
import dev.vcourt.engine.driver.Hello;
import dev.vcourt.engine.driver.StreamTransport;
import rikka.shizuku.Shizuku;

/**
 * The F2a proof: start the driver inside this APK through Shizuku, then greet it, read
 * V Court's own screen, tap V Court's test target, stop the driver and check it is gone.
 *
 * <p>Each step reports one line; the report holds no personal data (it reads only V Court's
 * screen), so it can be pasted as evidence. Cleanup runs whatever step failed.
 */
final class OneApkProof implements Runnable {
    /** Receives each line of the report, on the proof's thread. */
    interface Listener {
        void report(String line);
    }

    private static final String TAG = "VCourtProof";
    private static final long BIND_TIMEOUT_MS = 10_000;
    private static final long CONNECT_TIMEOUT_MS = 10_000;
    private static final long TAP_TIMEOUT_MS = 2_000;
    private static final long EXIT_TIMEOUT_MS = 5_000;
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
    private ShellBridge bridge;
    private DriverClient client;
    private boolean stopped;

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
            cleanUp();
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

        // Never take over another driver: the separate development APK serves the same name.
        if (socketAnswers()) {
            throw new Failure("Socket: another driver already serves " + DriverContract.SOCKET
                    + "; stop it first");
        }
        pass("Socket: " + DriverContract.SOCKET + " free");

        bridge = ShellBridge.bind(BIND_TIMEOUT_MS);
        pass("Shell service: bound, uid " + bridge.state().uid);

        String error = bridge.startDriver();
        if (error != null) {
            throw new Failure("am instrument: " + error);
        }
        long started = SystemClock.elapsedRealtime();
        LocalSocket socket = connectToStartedDriver();
        client = new DriverClient(new StreamTransport(socket.getInputStream(),
                socket.getOutputStream(), socket));
        pass("Driver: serving after " + (SystemClock.elapsedRealtime() - started) + " ms");

        long t = SystemClock.elapsedRealtime();
        Hello hello = client.hello();
        String ownVersion = context.getPackageManager()
                .getPackageInfo(DriverContract.PACKAGE, 0).versionName;
        if (!hello.version().equals(ownVersion)) {
            throw new Failure("hello: driver " + hello.version() + " is not this APK's "
                    + ownVersion);
        }
        pass("hello: " + hello + ", " + since(t));

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
        if (!waitFor(() -> taps.getAsInt() > before, TAP_TIMEOUT_MS)) {
            throw new Failure("click: sent (" + click.how() + ") but the target saw no tap");
        }
        pass("click: " + click.how() + " on " + click.bounds() + ", target saw it, "
                + clickTime + " ms");

        client.stop();
        stopped = true;
        if (!waitFor(() -> !bridge.state().running, EXIT_TIMEOUT_MS)) {
            throw new Failure("stop: am still runs after " + EXIT_TIMEOUT_MS + " ms");
        }
        ShellBridge.State state = bridge.state();
        pass("stop: am ended with " + state.exitCode + " (" + lastLine(state.output) + ")");
        if (socketAnswers()) {
            throw new Failure("stop: " + DriverContract.SOCKET + " still answers");
        }
        pass("stop: " + DriverContract.SOCKET + " closed; app pid still " + Process.myPid());
    }

    /** Connect once the driver serves; fail at once if {@code am} ended first. */
    private LocalSocket connectToStartedDriver() throws Exception {
        long deadline = SystemClock.elapsedRealtime() + CONNECT_TIMEOUT_MS;
        while (true) {
            try {
                return connect();
            } catch (IOException notYet) {
                ShellBridge.State state = bridge.state();
                if (!state.running) {
                    throw new Failure("am instrument ended with " + state.exitCode + ": "
                            + oneLine(state.output));
                }
                if (SystemClock.elapsedRealtime() > deadline) {
                    throw new Failure("Driver: no socket within " + CONNECT_TIMEOUT_MS
                            + " ms; am says: " + oneLine(state.output));
                }
                Thread.sleep(POLL_MS);
            }
        }
    }

    /** Stop what the proof started, whichever step failed. */
    private void cleanUp() {
        if (client != null) {
            if (!stopped && !client.isBroken()) {
                try {
                    client.stop();
                    stopped = true;
                    report("cleanup: driver asked to stop");
                } catch (DriverException e) {
                    report("cleanup: stop failed: " + e.getMessage());
                }
            }
            client.close();
        }
        if (bridge != null) {
            try {
                if (!waitFor(() -> !bridge.state().running, EXIT_TIMEOUT_MS)) {
                    bridge.killDriver();
                    report("cleanup: am killed; restart V Court before the next run");
                }
            } catch (Exception e) {
                report("cleanup: " + e);
            }
            bridge.unbind();
            report("cleanup: shell service unbound");
        }
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

    private interface Check {
        boolean holds() throws Exception;
    }

    private static boolean waitFor(Check check, long timeoutMillis) throws Exception {
        long deadline = SystemClock.elapsedRealtime() + timeoutMillis;
        while (!check.holds()) {
            if (SystemClock.elapsedRealtime() > deadline) {
                return false;
            }
            Thread.sleep(POLL_MS);
        }
        return true;
    }

    private static LocalSocket connect() throws IOException {
        LocalSocket socket = new LocalSocket();
        try {
            socket.connect(new LocalSocketAddress(DriverContract.SOCKET));
            return socket;
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    /** Tell whether anything serves the driver socket now. */
    private static boolean socketAnswers() {
        try {
            connect().close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static String since(long start) {
        return (SystemClock.elapsedRealtime() - start) + " ms";
    }

    private static String lastLine(String output) {
        String trimmed = output.trim();
        return trimmed.isEmpty() ? "no output" : trimmed.substring(trimmed.lastIndexOf('\n') + 1);
    }

    private static String oneLine(String output) {
        String trimmed = output.trim();
        return trimmed.isEmpty() ? "no output" : trimmed.replace('\n', ' ');
    }

    private void pass(String line) {
        report("PASS " + line);
    }

    private void report(String line) {
        Log.i(TAG, line);
        listener.report(line);
    }
}
