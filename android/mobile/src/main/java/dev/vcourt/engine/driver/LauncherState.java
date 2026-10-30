package dev.vcourt.engine.driver;

/** What a {@link DriverLauncher} knows of the process it started. */
public final class LauncherState {
    private final boolean running;
    private final Integer exitCode;
    private final String output;

    public LauncherState(boolean running, Integer exitCode, String output) {
        this.running = running;
        this.exitCode = exitCode;
        this.output = output == null ? "" : output;
    }

    /** Tell whether the process still runs; the driver serves only while it does. */
    public boolean running() {
        return running;
    }

    /** Return the process's exit code, or {@code null} while it runs or before it ran. */
    public Integer exitCode() {
        return exitCode;
    }

    /** Return the last lines the process printed, joined by line breaks; may be empty. */
    public String output() {
        return output;
    }

    /** Return the output on one line, or "no output". */
    public String outputLine() {
        String trimmed = output.trim();
        return trimmed.isEmpty() ? "no output" : trimmed.replace('\n', ' ');
    }
}
