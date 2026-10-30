package dev.vcourt.engine.driver;

/**
 * The driver answered a command with {@code ok: false}. The connection stays usable: the
 * driver read the command and replied in order.
 */
public final class DriverRefusedException extends DriverException {
    private static final long serialVersionUID = 1L;

    private final String op;
    private final String reason;

    public DriverRefusedException(String op, String reason) {
        super("The driver refused " + op + ": " + reason);
        this.op = op;
        this.reason = reason;
    }

    /** Return the command the driver refused. */
    public String op() {
        return op;
    }

    /** Return the driver's {@code error}, or {@code null} when it gave none. */
    public String reason() {
        return reason;
    }
}
