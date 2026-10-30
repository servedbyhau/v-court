package dev.vcourt.engine.screen;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Element bounds in pixels: left, top, right, bottom. */
public final class Box {
    private static final Pattern NUMBER = Pattern.compile("-?\\d+");

    public final int left;
    public final int top;
    public final int right;
    public final int bottom;

    public Box(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    /** Parse a dump's {@code bounds} such as {@code [35,740][79,784]}; null if malformed. */
    public static Box parse(String text) {
        if (text == null) {
            return null;
        }
        int[] numbers = new int[4];
        int count = 0;
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            if (count == 4) {
                return null;
            }
            numbers[count++] = Integer.parseInt(m.group());
        }
        return count == 4 ? new Box(numbers[0], numbers[1], numbers[2], numbers[3]) : null;
    }

    public int centerX() {
        return Math.floorDiv(left + right, 2);
    }

    public int centerY() {
        return Math.floorDiv(top + bottom, 2);
    }

    public int width() {
        return right - left;
    }

    public int height() {
        return bottom - top;
    }

    /** Tell whether this box lies within {@code outer}, edges included. */
    public boolean isInside(Box outer) {
        return outer.left <= left && outer.top <= top && right <= outer.right
                && bottom <= outer.bottom;
    }

    /** Tell whether {@code (x, y)} lies within this box, edges included. */
    public boolean contains(int x, int y) {
        return left <= x && x <= right && top <= y && y <= bottom;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Box)) {
            return false;
        }
        Box b = (Box) other;
        return left == b.left && top == b.top && right == b.right && bottom == b.bottom;
    }

    @Override
    public int hashCode() {
        return ((left * 31 + top) * 31 + right) * 31 + bottom;
    }

    /** Return the bounds as a dump writes them, {@code [l,t][r,b]}. */
    @Override
    public String toString() {
        return "[" + left + "," + top + "][" + right + "," + bottom + "]";
    }
}
