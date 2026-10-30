package dev.vcourt.engine.flow;

/** The app's notice says the apartment's quota is used up: no later round can book either. */
public final class QuotaFullException extends BookingRejectedException {
    private static final long serialVersionUID = 1L;

    public QuotaFullException(String message) {
        super(message);
    }

    public QuotaFullException(String message, Throwable cause) {
        super(message, cause);
    }
}
