package dev.vcourt.engine.driver;

/**
 * Something already serves the driver socket that this session did not start. V Court never
 * takes over or stops it: another tool, or the separate development driver, owns it.
 */
public final class DriverInUseException extends DriverUnavailableException {
    private static final long serialVersionUID = 1L;

    public DriverInUseException(String message) {
        super(message);
    }
}
