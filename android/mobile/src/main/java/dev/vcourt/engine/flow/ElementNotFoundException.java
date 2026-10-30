package dev.vcourt.engine.flow;

/** A required element did not show within the timeout. */
public final class ElementNotFoundException extends BookingException {
    private static final long serialVersionUID = 1L;

    public ElementNotFoundException(String message) {
        super(message);
    }

    public ElementNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
