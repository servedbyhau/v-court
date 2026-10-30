package dev.vcourt.engine.flow;

/** A tap did not lead to the expected screen. */
public final class NavigationException extends BookingException {
    private static final long serialVersionUID = 1L;

    public NavigationException(String message) {
        super(message);
    }

    public NavigationException(String message, Throwable cause) {
        super(message, cause);
    }
}
