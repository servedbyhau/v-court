package dev.vcourt.engine.screen;

/** An element of the screen: its accessibility label, visible bounds and clickability. */
public final class Node {
    public final String desc;
    public final Box box;
    public final boolean clickable;

    public Node(String desc, Box box, boolean clickable) {
        this.desc = desc == null ? "" : desc;
        this.box = box;
        this.clickable = clickable;
    }

    /** Return the tap point's x: the middle of the visible bounds. */
    public int centerX() {
        return box.centerX();
    }

    /** Return the tap point's y: the middle of the visible bounds. */
    public int centerY() {
        return box.centerY();
    }

    public int height() {
        return box.height();
    }

    /** Nodes are equal when label, bounds and clickability are: a screen read again equals. */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Node)) {
            return false;
        }
        Node n = (Node) other;
        return clickable == n.clickable && desc.equals(n.desc) && box.equals(n.box);
    }

    @Override
    public int hashCode() {
        return (desc.hashCode() * 31 + box.hashCode()) * 2 + (clickable ? 1 : 0);
    }

    @Override
    public String toString() {
        return box + (clickable ? " clickable " : " ") + "'" + desc.replace("\n", "\\n") + "'";
    }
}
