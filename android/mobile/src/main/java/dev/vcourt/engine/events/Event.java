package dev.vcourt.engine.events;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** One step of a run: its kind, when it happened and its details (JSON-friendly values). */
public final class Event {
    public final EventKind kind;
    public final LocalDateTime time;
    public final Map<String, Object> data;

    public Event(EventKind kind, LocalDateTime time, Map<String, Object> data) {
        this.kind = kind;
        this.time = time;
        this.data = Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }
}
