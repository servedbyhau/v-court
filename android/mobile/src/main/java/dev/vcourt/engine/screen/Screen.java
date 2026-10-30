package dev.vcourt.engine.screen;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * The elements of one hierarchy dump, in document order. A dump returns every element on
 * the screen with its bounds in about the time of one element query, so the flow reads each
 * screen once and finds what it needs locally. Ported from the desktop's {@code screen.py}.
 */
public final class Screen {
    private final List<Node> nodes;

    public Screen(List<Node> nodes) {
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
    }

    /**
     * Build a screen from a dump; nodes without bounds are skipped.
     *
     * @throws IllegalArgumentException when {@code xml} is not well-formed
     */
    public static Screen parse(String xml) {
        List<Node> found = new ArrayList<>();
        DefaultHandler handler = new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName,
                    Attributes attributes) {
                if (!"node".equals(qName)) {
                    return;
                }
                Box box = Box.parse(attributes.getValue("bounds"));
                if (box != null) {
                    found.add(new Node(attributes.getValue("content-desc"), box,
                            "true".equals(attributes.getValue("clickable"))));
                }
            }
        };
        try {
            SAXParserFactory.newInstance().newSAXParser()
                    .parse(new InputSource(new StringReader(xml)), handler);
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new IllegalArgumentException("Not a hierarchy dump: " + e.getMessage(), e);
        }
        return new Screen(found);
    }

    public List<Node> nodes() {
        return nodes;
    }

    /**
     * Return the width covered by the elements, the screen's: read from the dump because
     * MuMu reports the landscape size when it runs a tablet resolution in portrait.
     */
    public int width() {
        int width = 0;
        for (Node node : nodes) {
            width = Math.max(width, node.box.right);
        }
        return width;
    }

    /** Return the height covered by the elements, the screen's. */
    public int height() {
        int height = 0;
        for (Node node : nodes) {
            height = Math.max(height, node.box.bottom);
        }
        return height;
    }

    /** Return the first element that meets {@code match}, or null. */
    public Node find(Match match) {
        for (Node node : nodes) {
            if (match.test(node)) {
                return node;
            }
        }
        return null;
    }

    /** Return every element that meets {@code match}. */
    public List<Node> findAll(Match match) {
        List<Node> all = new ArrayList<>();
        for (Node node : nodes) {
            if (match.test(node)) {
                all.add(node);
            }
        }
        return all;
    }

    /**
     * Return the line after {@code caption} within an element's label: the app puts a
     * caption and its value on two lines of one label, such as
     * {@code Vị trí/dịch vụ\nS6 - Sân tennis}. Null when no label has {@code caption} on a
     * line of its own with a line after it.
     */
    public String lineAfter(String caption) {
        for (Node node : nodes) {
            String[] lines = node.desc.split("\n", -1);
            for (int i = 0; i < lines.length - 1; i++) {
                if (lines[i].equals(caption)) {
                    return lines[i + 1];
                }
            }
        }
        return null;
    }

    /**
     * Return one log line per labelled or clickable element: unlabelled clickable ones stay,
     * since some buttons, such as the calendar's month arrows, have no label.
     */
    public List<String> describe() {
        List<String> lines = new ArrayList<>();
        for (Node node : nodes) {
            if (!node.desc.isEmpty() || node.clickable) {
                lines.add(node.toString());
            }
        }
        return lines;
    }
}
