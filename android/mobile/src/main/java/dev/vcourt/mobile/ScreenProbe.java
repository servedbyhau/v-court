package dev.vcourt.mobile;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Counts what a driver dump holds and finds one label, without keeping any text: another
 * app's screen may show personal data, so only counts and found labels leave this class.
 * Labels match {@code content-desc} exactly, as the desktop's {@code screen.py} does.
 */
final class ScreenProbe {
    private static final Pattern BOUNDS =
            Pattern.compile("\\[(-?\\d+),(-?\\d+)]\\[(-?\\d+),(-?\\d+)]");

    /** Nodes in the dump. */
    int nodes;
    /** Nodes with a non-empty {@code content-desc}: 0 while a Flutter app shows a splash. */
    int labelled;
    /** The first wanted label found, or {@code null}. */
    String found;
    /** Its bounds as {@code [l,t][r,b]}, or {@code null}. */
    String bounds;
    /** Its centre. */
    int x;
    int y;

    private ScreenProbe() {
    }

    /** Read {@code xml} and look for the first node labelled one of {@code labels}. */
    static ScreenProbe read(String xml, Collection<String> labels)
            throws XmlPullParserException, IOException {
        ScreenProbe probe = new ScreenProbe();
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new StringReader(xml));
        for (int event = parser.getEventType(); event != XmlPullParser.END_DOCUMENT;
                event = parser.next()) {
            if (event != XmlPullParser.START_TAG || !"node".equals(parser.getName())) {
                continue;
            }
            probe.nodes++;
            String desc = parser.getAttributeValue(null, "content-desc");
            if (desc == null || desc.isEmpty()) {
                continue;
            }
            probe.labelled++;
            if (probe.found == null && labels.contains(desc)) {
                Matcher m = BOUNDS.matcher(parser.getAttributeValue(null, "bounds"));
                if (m.matches()) {
                    probe.found = desc;
                    probe.bounds = m.group();
                    probe.x = (Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(3))) / 2;
                    probe.y = (Integer.parseInt(m.group(2)) + Integer.parseInt(m.group(4))) / 2;
                }
            }
        }
        return probe;
    }
}
