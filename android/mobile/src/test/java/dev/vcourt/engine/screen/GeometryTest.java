package dev.vcourt.engine.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** Ported from the desktop's {@code tests/test_geometry.py}. */
class GeometryTest {
    private static final int SCREEN_WIDTH = 540;
    private static final Box CAPTION = new Box(79, 739, 313, 772); // captured at 540x960
    private static final Box CHECKBOX = new Box(35, 740, 79, 784);

    @Test
    void parseBounds() {
        assertEquals(CHECKBOX, Box.parse("[35,740][79,784]"));
        assertNull(Box.parse(""));
        assertNull(Box.parse("[1,2][3,4][5,6]"));
    }

    @Test
    void isInsideIncludesTheEdges() {
        Box dialog = new Box(0, 33, 540, 1600);
        assertTrue(new Box(47, 841, 493, 936).isInside(dialog));
        assertTrue(dialog.isInside(dialog));
        assertFalse(new Box(6, 5, 50, 28).isInside(dialog)); // the status bar clock above it
    }

    @Test
    void checkboxBesideCaptionIsAccepted() {
        assertTrue(Geometry.isCheckboxBeside(CHECKBOX, CAPTION, SCREEN_WIDTH));
    }

    @Test
    void unrelatedNodesAreRejected() {
        Box backButton = new Box(0, 40, 60, 100);
        Box container = new Box(0, 721, 540, 869);
        Box fullScreen = new Box(0, 0, 540, 960);
        for (Box box : new Box[] {backButton, container, fullScreen}) {
            assertFalse(Geometry.isCheckboxBeside(box, CAPTION, SCREEN_WIDTH));
        }
    }

    @Test
    void checkboxDetectionScalesWithResolution() {
        double factor = 1440.0 / SCREEN_WIDTH;

        assertTrue(Geometry.isCheckboxBeside(scale(CHECKBOX, factor), scale(CAPTION, factor),
                1440));
    }

    private static Box scale(Box box, double factor) {
        return new Box((int) (box.left * factor), (int) (box.top * factor),
                (int) (box.right * factor), (int) (box.bottom * factor));
    }

    @Test
    void parseBookedTimeSplitsDateAndSlot() {
        Geometry.BookedTime time = Geometry.parseBookedTime("17/10/2026 15:00 - 16:00");
        assertEquals(LocalDate.of(2026, 10, 17), time.date);
        assertEquals("15:00 - 16:00", time.slot);
        assertEquals(LocalDate.of(2026, 11, 1),
                Geometry.parseBookedTime("1/11/2026 06:00 - 07:00").date);
        assertNull(Geometry.parseBookedTime("31/11/2026 15:00 - 16:00"));
        assertNull(Geometry.parseBookedTime("S6 - Sân tennis"));
    }

    @Test
    void monthIndexGrowsByOnePerMonth() {
        assertEquals(Geometry.monthIndex(2026, 12) + 1, Geometry.monthIndex(2027, 1));
    }
}
