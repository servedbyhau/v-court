package dev.vcourt.engine.flow;

/** The date or slot did not show before the retry window ended. */
public final class BookingNotOpenException extends BookingException {
    private static final long serialVersionUID = 1L;

    public BookingNotOpenException(String message) {
        super(message);
    }

    public BookingNotOpenException(String message, Throwable cause) {
        super(message, cause);
    }
}
