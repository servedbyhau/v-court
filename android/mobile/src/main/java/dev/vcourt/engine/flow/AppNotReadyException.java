package dev.vcourt.engine.flow;

/** The app did not reach its home or utilities screen, such as after a logout. */
public final class AppNotReadyException extends CourtBookerException {
    private static final long serialVersionUID = 1L;

    public AppNotReadyException(String message) {
        super(message);
    }

    public AppNotReadyException(String message, Throwable cause) {
        super(message, cause);
    }
}
