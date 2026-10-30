package dev.vcourt.engine.driver;

/**
 * The driver did not answer a command in time. Its late reply would answer the next
 * command, so the connection is lost: a run that sees this has lost the race, as a frozen
 * emulator does on the desktop.
 */
public final class DriverNotRespondingException extends DriverException {
    private static final long serialVersionUID = 1L;

    public DriverNotRespondingException(String message, Throwable cause) {
        super(message, cause);
    }
}
