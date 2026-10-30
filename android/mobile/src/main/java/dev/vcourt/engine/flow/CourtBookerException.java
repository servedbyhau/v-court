package dev.vcourt.engine.flow;

/**
 * Base of the run's errors, after the desktop's {@code errors.py}: a {@link BookingException}
 * ends one round and later rounds still run (but none after {@link QuotaFullException});
 * the others stop the whole run.
 */
public class CourtBookerException extends Exception {
    private static final long serialVersionUID = 1L;

    public CourtBookerException(String message) {
        super(message);
    }

    public CourtBookerException(String message, Throwable cause) {
        super(message, cause);
    }
}
