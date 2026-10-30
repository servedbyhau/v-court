package dev.vcourt.engine.driver;

/** The driver refused a command, or its connection can no longer carry one. */
public class DriverException extends Exception {
    private static final long serialVersionUID = 1L;

    public DriverException(String message) {
        super(message);
    }

    public DriverException(String message, Throwable cause) {
        super(message, cause);
    }
}
