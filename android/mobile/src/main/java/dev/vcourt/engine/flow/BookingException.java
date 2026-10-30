package dev.vcourt.engine.flow;

/** An error that ends one booking round; later rounds still run. */
public class BookingException extends CourtBookerException {
    private static final long serialVersionUID = 1L;

    public BookingException(String message) {
        super(message);
    }

    public BookingException(String message, Throwable cause) {
        super(message, cause);
    }
}
