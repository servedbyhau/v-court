package dev.vcourt.engine.driver;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeoutException;

/** A transport that answers each receive from a script and records what was sent. */
final class FakeTransport implements DriverTransport {
    /** Script entry: the connection ends. */
    static final Object EOF = new Object();
    /** Script entry: no reply within the timeout. */
    static final Object TIMEOUT = new Object();

    final List<String> sent = new ArrayList<>();
    final List<Long> timeouts = new ArrayList<>();
    private final Deque<Object> script = new ArrayDeque<>();
    boolean closed;

    /**
     * Answer the next receive with {@code entry}: a line, {@link #EOF}, {@link #TIMEOUT} or
     * an IOException.
     */
    FakeTransport then(Object entry) {
        script.add(entry);
        return this;
    }

    @Override
    public void send(String line) throws IOException {
        if (closed) {
            throw new IOException("closed");
        }
        sent.add(line);
    }

    @Override
    public String receive(long timeoutMillis) throws IOException, TimeoutException {
        timeouts.add(timeoutMillis);
        Object entry = script.poll();
        if (entry == null) {
            throw new AssertionError("The client read more replies than the test scripted");
        }
        if (entry == EOF) {
            return null;
        }
        if (entry == TIMEOUT) {
            throw new TimeoutException("scripted");
        }
        if (entry instanceof IOException) {
            throw (IOException) entry;
        }
        return (String) entry;
    }

    @Override
    public void close() {
        closed = true;
    }
}
