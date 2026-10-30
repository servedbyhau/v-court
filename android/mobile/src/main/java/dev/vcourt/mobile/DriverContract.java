package dev.vcourt.mobile;

import java.util.List;

/**
 * How the one V Court APK holds and starts its driver: the instrumentation declared in the
 * manifest, the socket it serves, V Court's own target the checks tap, and the app they read.
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
    /** The Vinhomes Resident app, which the driver reads and later books in. */
    static final String VINHOMES = "com.vinhomes.resident";
    /**
     * The label of the home screen's Utilities tile in Vietnamese and English (desktop
     * {@code app_language.py}, {@code home}): the home screen shows once one of them does.
     */
    static final List<String> HOME_LABELS = List.of("Tiện ích", "Utilities");

    private DriverContract() {
    }
}
