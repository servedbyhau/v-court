package dev.vcourt.engine.flow;

/** The slot, venue or court is fully booked. */
public final class SlotUnavailableException extends BookingException {
    private static final long serialVersionUID = 1L;

    public SlotUnavailableException(String message) {
        super(message);
    }

    public SlotUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
