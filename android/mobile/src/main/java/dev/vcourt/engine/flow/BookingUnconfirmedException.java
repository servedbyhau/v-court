package dev.vcourt.engine.flow;

/** Neither the ticket nor a notice showed in time after the confirmation: the outcome is unknown. */
public final class BookingUnconfirmedException extends BookingException {
    private static final long serialVersionUID = 1L;

    public BookingUnconfirmedException(String message) {
        super(message);
    }

    public BookingUnconfirmedException(String message, Throwable cause) {
        super(message, cause);
    }
}
