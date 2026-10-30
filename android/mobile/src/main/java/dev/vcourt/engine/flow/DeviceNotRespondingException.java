package dev.vcourt.engine.flow;

/**
 * A device command did not end in time: the device or its driver froze. The run ends after
 * the round it happened in, since no later round would get an answer either.
 */
public final class DeviceNotRespondingException extends DeviceException {
    private static final long serialVersionUID = 1L;

    public DeviceNotRespondingException(String message) {
        super(message);
    }

    public DeviceNotRespondingException(String message, Throwable cause) {
        super(message, cause);
    }
}
