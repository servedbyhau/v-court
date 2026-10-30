package dev.vcourt.engine.flow;

/** The app answered with a notice instead of the next screen, such as the ticket. */
public class BookingRejectedException extends BookingException {
    private static final long serialVersionUID = 1L;

    public BookingRejectedException(String message) {
        super(message);
    }

    public BookingRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
