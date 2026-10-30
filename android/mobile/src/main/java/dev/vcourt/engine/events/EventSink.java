package dev.vcourt.engine.events;

/** Receives every event of a run. */
public interface EventSink {
    void emit(Event event) throws Exception;
}
