package dev.vcourt.mobile;

/**
 * How the one V Court APK holds and starts its driver: the instrumentation declared in the
 * manifest, the socket it serves and V Court's own target the proof taps.
 */
final class DriverContract {
    static final String PACKAGE = "dev.vcourt.mobile";
    /** The driver class, compiled from {@code android/app} into this APK. */
    static final String INSTRUMENTATION = "dev.courtbooker.driver.Driver";
    /** What {@code am instrument} starts; the instrumentation targets this same package. */
    static final String COMPONENT = PACKAGE + "/" + INSTRUMENTATION;
    /** The abstract socket the driver serves; the same name as the desktop driver's. */
    static final String SOCKET = "court-driver";
    /** The content description of the button inside V Court that the proof taps. */
    static final String TEST_TARGET = "V Court test target";

    private DriverContract() {
    }
}
