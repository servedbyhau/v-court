package dev.vcourt.mobile;

import android.os.SystemClock;

import java.util.List;
import java.util.function.IntSupplier;

import dev.vcourt.engine.driver.ClickResult;
import dev.vcourt.engine.driver.DriverClient;
import dev.vcourt.engine.driver.DriverSession;
import dev.vcourt.engine.driver.LauncherState;

/** What the in-app driver checks share: their report, failures and V Court's test target. */
final class CheckSupport {
    /** Receives each line of a check's report, on the check's thread. */
    interface Listener {
        void report(String line);
    }

    /** A step that did not give what the check needs; its message says which and why. */
    static final class Failure extends Exception {
        private static final long serialVersionUID = 1L;

        Failure(String message) {
            super(message);
        }
    }

    private static final long TAP_TIMEOUT_MS = 2_000;
    private static final long POLL_MS = 100;

    private CheckSupport() {
    }

    /**
     * Read V Court's screen, click its test target through the driver and wait until the
     * target counted the tap; return the report line.
     *
     * @throws Failure when the target is not shown, not clicked or saw no tap
     */
    static String tapTestTarget(DriverClient client, IntSupplier taps) throws Exception {
        long t = SystemClock.elapsedRealtime();
        String xml = client.dump();
        long dumpTime = SystemClock.elapsedRealtime() - t;
        ScreenProbe target = ScreenProbe.read(xml, List.of(DriverContract.TEST_TARGET));
        if (target.found == null) {
            throw new Failure("dump: " + target.nodes + " nodes, no \"" + DriverContract.TEST_TARGET
                    + "\"");
        }
        int before = taps.getAsInt();
        t = SystemClock.elapsedRealtime();
        ClickResult click = client.click(DriverContract.TEST_TARGET, target.x, target.y);
        if (!click.clicked()) {
            throw new Failure("click: the driver found no target");
        }
        long clickTime = SystemClock.elapsedRealtime() - t;
        long deadline = SystemClock.elapsedRealtime() + TAP_TIMEOUT_MS;
        while (taps.getAsInt() <= before) {
            if (SystemClock.elapsedRealtime() > deadline) {
                throw new Failure("click: sent (" + click.how() + ") but the target saw no tap");
            }
            Thread.sleep(POLL_MS);
        }
        return "dump " + xml.length() + " chars, " + target.nodes + " nodes in " + dumpTime
                + " ms; click " + click.how() + " on " + click.bounds() + " in " + clickTime
                + " ms, target saw it";
    }

    /** Return how a session ended, on one line. */
    static String describe(DriverSession.StopReport stopped) {
        LauncherState state = stopped.state();
        StringBuilder line =
                new StringBuilder(stopped.asked() ? "driver stopped" : "stop not delivered");
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

    static String since(long start) {
        return (SystemClock.elapsedRealtime() - start) + " ms";
    }

    private static String lastLine(String output) {
        String trimmed = output.trim();
        return trimmed.isEmpty() ? "no output" : trimmed.substring(trimmed.lastIndexOf('\n') + 1);
    }
}
