package dev.vcourt.engine.events;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stamps events with the time and hands them to sinks. A failing sink is logged and
 * otherwise ignored: reporting must never cost a booking. Ported from the desktop's
 * {@code events.Reporter}.
 */
public final class Reporter {
    private static final Logger LOG = Logger.getLogger(Reporter.class.getName());

    private final List<EventSink> sinks;
    private final Supplier<LocalDateTime> clock;

    public Reporter(EventSink... sinks) {
        this(LocalDateTime::now, sinks);
    }

    public Reporter(Supplier<LocalDateTime> clock, EventSink... sinks) {
        this.clock = clock;
        this.sinks = List.of(sinks);
    }

    /** Report {@code kind} with data given as key, value, key, value. */
    public void report(EventKind kind, Object... keysAndValues) {
        Map<String, Object> data = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keysAndValues.length; i += 2) {
            data.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        report(kind, data);
    }

    public void report(EventKind kind, Map<String, Object> data) {
        Event event = new Event(kind, clock.get(), data);
        for (EventSink sink : sinks) {
            try {
                sink.emit(event);
            } catch (Exception | LinkageError e) {
                LOG.log(Level.WARNING, "Event sink failed on " + kind.value(), e);
            }
        }
    }
}
