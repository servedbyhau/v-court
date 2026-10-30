package dev.vcourt.engine.events;

import java.util.ArrayList;
import java.util.List;

/** Keeps events in a list: for tests, and for the run's own summary. */
public final class MemorySink implements EventSink {
    public final List<Event> events = new ArrayList<>();

    @Override
    public synchronized void emit(Event event) {
        events.add(event);
    }

    /** Return the kind of every event received, in order. */
    public synchronized List<EventKind> kinds() {
        List<EventKind> kinds = new ArrayList<>();
        for (Event event : events) {
            kinds.add(event.kind);
        }
        return kinds;
    }
}
