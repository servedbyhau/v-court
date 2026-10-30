package dev.vcourt.engine.driver;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

/** Answers each connect from a script; nothing serves the socket once it runs out. */
final class FakeConnector implements DriverConnector {
    /** Script entry: nothing serves the socket. */
    static final Object FREE = new Object();

    int connects;
    private final Deque<Object> script = new ArrayDeque<>();

    /** Answer the next connect with {@code entry}: a {@link FakeTransport} or {@link #FREE}. */
    FakeConnector then(Object entry) {
        script.add(entry);
        return this;
    }

    @Override
    public DriverTransport connect() throws IOException {
        connects++;
        Object entry = script.poll();
        if (entry == null || entry == FREE) {
            throw new IOException("Connection refused");
        }
        return (DriverTransport) entry;
    }
}
