package dev.vcourt.engine.flow;

/** The device, or the driver behind it, is not usable: the run stops. */
public class DeviceException extends CourtBookerException {
    private static final long serialVersionUID = 1L;

    public DeviceException(String message) {
        super(message);
    }

    public DeviceException(String message, Throwable cause) {
        super(message, cause);
    }
}
