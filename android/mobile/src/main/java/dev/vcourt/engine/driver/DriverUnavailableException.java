package dev.vcourt.engine.driver;

/** No driver session could be opened: the driver did not start, answer or match this app. */
public class DriverUnavailableException extends DriverException {
    private static final long serialVersionUID = 1L;

    public DriverUnavailableException(String message) {
        super(message);
    }

    public DriverUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
