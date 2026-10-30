package dev.vcourt.engine.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Ported from the desktop's {@code tests/test_screen.py}. */
class ScreenTest {
    static final String DUMP = "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n"
            + "<hierarchy rotation=\"0\">\n"
            + "  <node content-desc=\"\" bounds=\"[0,0][540,1600]\" clickable=\"false\">\n"
            + "    <node content-desc=\"18:00 - 19:00&#10;Hết chỗ\" bounds=\"[38,1178][263,1262]\""
            + " clickable=\"true\" />\n"
            + "    <node content-desc=\"Tiếp tục\" bounds=\"[35,1490][505,1584]\""
            + " clickable=\"true\" />\n"
            + "    <node content-desc=\"Xác nhận\" bounds=\"[35,1490][505,1584]\""
            + " clickable=\"false\" />\n"
            + "    <node content-desc=\"no bounds\" />\n"
            + "  </node>\n"
            + "</hierarchy>";

    private static List<String> descs(Screen screen) {
        List<String> descs = new ArrayList<>();
        for (Node node : screen.nodes()) {
            descs.add(node.desc);
        }
        return descs;
    }

    @Test
    void parseKeepsLabelsBoundsAndClickable() {
        Screen screen = Screen.parse(DUMP);

        assertEquals(List.of("", "18:00 - 19:00\nHết chỗ", "Tiếp tục", "Xác nhận"),
                descs(screen));
        assertEquals(new Box(38, 1178, 263, 1262), screen.nodes().get(1).box);
        assertTrue(screen.nodes().get(2).clickable);
        assertFalse(screen.nodes().get(3).clickable);
    }

    @Test
    void parseReadsTheDriversDumpWithoutDeclaration() {
        Screen screen = Screen.parse("<hierarchy><node text=\"\" class=\"android.view.View\""
                + " content-desc=\"A &amp; B\" clickable=\"true\" enabled=\"true\""
                + " bounds=\"[1,2][3,4]\"></node></hierarchy>");

        assertEquals(List.of("A & B"), descs(screen));
    }

    @Test
    void parseRefusesWhatIsNotADump() {
        assertThrows(IllegalArgumentException.class, () -> Screen.parse("<hierarchy>"));
    }

    @Test
    void sizeIsReadFromTheDump() {
        Screen screen = Screen.parse(DUMP);

        assertEquals(540, screen.width());
        assertEquals(1600, screen.height());
        assertEquals(0, new Screen(Collections.emptyList()).width());
        assertEquals(0, new Screen(Collections.emptyList()).height());
    }

    @Test
    void matchCriteria() {
        Node slot = new Node("18:00 - 19:00\nHết chỗ", new Box(38, 1178, 263, 1262), true);

        assertTrue(Match.prefix("18:00 - 19:00").test(slot));
        assertTrue(Match.contains("Hết chỗ").test(slot));
        assertTrue(Match.pattern("\\d{2}:\\d{2} - \\d{2}:\\d{2}.*").test(slot));
        assertFalse(Match.pattern("\\d{2}:\\d{2}").test(slot));
        assertFalse(Match.desc("18:00 - 19:00").test(slot));
        assertFalse(Match.prefix("18:00 - 19:00").withClickable(false).test(slot));
    }

    @Test
    void findRespectsClickable() {
        Screen screen = Screen.parse(DUMP);

        assertNull(screen.find(Match.desc("Xác nhận").withClickable(true)));
        Node next = screen.find(Match.desc("Tiếp tục"));
        assertEquals(270, next.centerX());
        assertEquals(1537, next.centerY());
        assertEquals(1, screen.findAll(Match.prefix("18:00")).size());
    }

    @Test
    void describeListsLabelledAndClickableElements() {
        List<Node> nodes = new ArrayList<>(Screen.parse(DUMP).nodes());
        nodes.add(new Node("", new Box(480, 180, 520, 220), true));

        assertEquals(List.of(
                "[38,1178][263,1262] clickable '18:00 - 19:00\\nHết chỗ'",
                "[35,1490][505,1584] clickable 'Tiếp tục'",
                "[35,1490][505,1584] 'Xác nhận'",
                "[480,180][520,220] clickable ''"), new Screen(nodes).describe());
        assertEquals(List.of(), new Screen(Collections.emptyList()).describe());
    }

    @Test
    void lineAfterReadsTheValueUnderACaption() {
        Node details = new Node("Sân Tennis\nVị trí/dịch vụ\nS6 - Sân tennis\nTổng tiền",
                new Box(0, 210, 540, 775), false);
        Screen screen = new Screen(List.of(
                new Node("Vị trí/dịch vụ", new Box(0, 0, 10, 10), false), details));

        assertEquals("S6 - Sân tennis", screen.lineAfter("Vị trí/dịch vụ"));
        assertNull(screen.lineAfter("Tổng tiền"));
        assertNull(screen.lineAfter("Vị trí"));
    }
}
