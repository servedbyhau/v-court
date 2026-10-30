package dev.vcourt.engine.driver;

/**
 * The driver's answer to a click. Nothing is clicked when the element is gone: whatever is
 * at its old place now is another element, so the flow reads the screen and clicks again.
 */
public final class ClickResult {
    private final boolean clicked;
    private final String how;
    private final String bounds;

    public ClickResult(boolean clicked, String how, String bounds) {
        this.clicked = clicked;
        this.how = how;
        this.bounds = bounds;
    }

    /** Tell whether an element was found and clicked. */
    public boolean clicked() {
        return clicked;
    }

    /**
     * Return {@code "action"} for an accessibility click, {@code "touch"} for a tap on the
     * element's middle, or {@code null} when nothing was clicked.
     */
    public String how() {
        return how;
    }

    /** Return the clicked element's bounds as {@code [l,t][r,b]}, or {@code null}. */
    public String bounds() {
        return bounds;
    }
}
