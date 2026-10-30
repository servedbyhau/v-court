package dev.vcourt.engine.flow;

/**
 * The device calls the booking flow makes, each one round trip to the device; on Android the
 * driver carries them. A tap names its element's label: the driver clicks the element with
 * that label where it is then, so a list that moved cannot send the tap to another element.
 */
public interface Device {
    /** Return the active window as a hierarchy dump. */
    String dump() throws DeviceException;

    /**
     * Click the element labelled {@code label} where it is now; {@code (x, y)}, where the last
     * read showed it, picks among elements with the same label. An empty label clicks the
     * smallest clickable element at {@code (x, y)}.
     */
    void click(int x, int y, String label) throws DeviceException;

    void back() throws DeviceException;

    void swipe(int x1, int y1, int x2, int y2, long millis) throws DeviceException;

    void startApp(String pkg) throws DeviceException;

    void stopApp(String pkg) throws DeviceException;

    /** Return the package of the active window, empty when there is none. */
    String currentPackage() throws DeviceException;

    /**
     * Turn off animations and notification banners, so screens change without transitions
     * and no banner covers the app. Best effort: a failure only costs those safeguards.
     */
    void quiet() throws DeviceException;
}
