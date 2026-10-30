package dev.vcourt.engine.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A scripted app, ported from the desktop's {@code FakeApp}: each screen is a list of nodes;
 * taps and Back switch screens, at once or after a number of dumps (a screen still loading).
 * A list of switches is followed in turn, each delay counted from the one before.
 */
final class FakeApp implements Device {
    static final int WIDTH = 540;
    static final int HEIGHT = 1600;

    /** A node of a scripted screen. */
    static final class N {
        final String desc;
        final int[] box;
        final boolean clickable;

        N(String desc, int[] box, boolean clickable) {
            this.desc = desc;
            this.box = box;
            this.clickable = clickable;
        }

        /** Return how taps name this node: its label, else its bounds. */
        String key() {
            return desc.isEmpty() ? box[0] + "," + box[1] + "," + box[2] + "," + box[3] : desc;
        }
    }

    /** A screen switch: to {@code screen} after {@code dumps} reads. */
    static final class Step {
        final String screen;
        int dumps;

        Step(String screen, int dumps) {
            this.screen = screen;
            this.dumps = dumps;
        }
    }

    final Map<String, List<N>> screens;
    /** (screen + "|" + tapped key) to the steps it starts. */
    final Map<String, List<Step>> tapsTo = new HashMap<>();
    final Map<String, String> backsTo;
    String screen;
    String launchesTo;
    String current = "";
    final List<Step> pending = new ArrayList<>();
    final Map<String, Integer> swallow = new HashMap<>();
    Consumer<FakeApp> onSwipe;
    final List<String> ops = new ArrayList<>();
    final List<String> taps = new ArrayList<>();
    final List<String> labels = new ArrayList<>();
    final List<String> calls = new ArrayList<>();
    int swipes;
    int quiets;

    FakeApp(Map<String, List<N>> screens, Map<String, String> backsTo, String screen) {
        this.screens = screens;
        this.backsTo = backsTo;
        this.screen = screen;
    }

    static N node(String desc, int l, int t, int r, int b) {
        return new N(desc, new int[] {l, t, r, b}, true);
    }

    static N label(String desc, int l, int t, int r, int b) {
        return new N(desc, new int[] {l, t, r, b}, false);
    }

    FakeApp tap(String from, String key, String to) {
        return tap(from, key, List.of(new Step(to, 0)));
    }

    FakeApp tap(String from, String key, List<Step> to) {
        tapsTo.put(from + "|" + key, to);
        return this;
    }

    void go(List<Step> steps) {
        pending.clear();
        for (Step step : steps) {
            pending.add(new Step(step.screen, step.dumps));
        }
        settle();
    }

    private void settle() {
        while (!pending.isEmpty() && pending.get(0).dumps <= 0) {
            screen = pending.remove(0).screen;
        }
    }

    @Override
    public String dump() {
        ops.add("dump");
        if (!pending.isEmpty()) {
            pending.get(0).dumps--;
            settle();
        }
        StringBuilder xml = new StringBuilder("<hierarchy><node content-desc=\"\" bounds=\"[0,0]["
                + WIDTH + "," + HEIGHT + "]\" clickable=\"false\">");
        for (N n : screens.get(screen)) {
            xml.append("<node content-desc=\"").append(escape(n.desc)).append("\" bounds=\"[")
                    .append(n.box[0]).append(',').append(n.box[1]).append("][")
                    .append(n.box[2]).append(',').append(n.box[3]).append("]\" clickable=\"")
                    .append(n.clickable).append("\" />");
        }
        return xml.append("</node></hierarchy>").toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\n", "&#10;");
    }

    @Override
    public void click(int x, int y, String label) {
        labels.add(label);
        N hit = null;
        for (N n : screens.get(screen)) {
            if (n.box[0] <= x && x <= n.box[2] && n.box[1] <= y && y <= n.box[3]
                    && (hit == null || area(n) < area(hit))) {
                hit = n;
            }
        }
        if (hit == null) {
            ops.add("click:none");
            return;
        }
        String key = hit.key();
        ops.add("click:" + key);
        taps.add(key);
        int swallowed = swallow.getOrDefault(key, 0);
        if (swallowed > 0) {
            swallow.put(key, swallowed - 1);
            return;
        }
        List<Step> target = tapsTo.get(screen + "|" + key);
        if (target != null) {
            go(target);
        }
    }

    private static int area(N n) {
        return (n.box[2] - n.box[0]) * (n.box[3] - n.box[1]);
    }

    @Override
    public void back() {
        calls.add("back");
        String target = backsTo.get(screen);
        if (target != null) {
            go(List.of(new Step(target, 0)));
        }
    }

    @Override
    public void swipe(int x1, int y1, int x2, int y2, long millis) {
        swipes++;
        if (onSwipe != null) {
            onSwipe.accept(this);
        }
    }

    @Override
    public void startApp(String pkg) {
        calls.add("app_start");
        if (launchesTo != null) {
            go(List.of(new Step(launchesTo, 0)));
        }
    }

    @Override
    public void stopApp(String pkg) {
        calls.add("app_stop");
    }

    @Override
    public String currentPackage() {
        return current;
    }

    @Override
    public void quiet() {
        quiets++;
    }
}
