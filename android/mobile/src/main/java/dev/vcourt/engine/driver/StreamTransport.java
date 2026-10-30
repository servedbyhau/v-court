package dev.vcourt.engine.driver;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link DriverTransport} over a pair of streams, such as a {@code LocalSocket}'s.
 *
 * <p>A reader thread takes each line off the input as it comes, so {@link #receive} can give
 * up after its timeout whether or not the stream supports one: a socket read cannot be
 * relied on to time out. The thread ends with the stream.
 */
public final class StreamTransport implements DriverTransport {
    /** Queued once the input ended; stays queued so every later read sees the end too. */
    private static final Object END = new Object();

    private final BufferedReader in;
    private final Writer out;
    private final Closeable resource;
    private final BlockingQueue<Object> lines = new LinkedBlockingQueue<>();
    private volatile boolean closed;

    /**
     * Read lines from {@code input} and write them to {@code output} as UTF-8.
     * {@code resource}, such as the socket the streams belong to, is closed with the
     * transport; {@code null} closes the streams alone.
     */
    public StreamTransport(InputStream input, OutputStream output, Closeable resource) {
        in = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        out = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        this.resource = resource;
        Thread reader = new Thread(this::readLines, "driver-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLines() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                lines.add(line);
            }
            lines.add(END);
        } catch (IOException e) {
            lines.add(closed ? END : e);
        }
    }

    @Override
    public synchronized void send(String line) throws IOException {
        if (line.indexOf('\n') >= 0 || line.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("A protocol line holds no line break");
        }
        if (closed) {
            throw new IOException("The transport is closed");
        }
        out.write(line);
        out.write('\n');
        out.flush();
    }

    @Override
    public String receive(long timeoutMillis) throws IOException, TimeoutException {
        Object item;
        try {
            item = lines.poll(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while waiting for the driver");
        }
        if (item == null) {
            throw new TimeoutException("No line within " + timeoutMillis + " ms");
        }
        if (item == END) {
            lines.add(END);
            return null;
        }
        if (item instanceof IOException) {
            IOException failure = (IOException) item;
            lines.add(END);
            throw new IOException(failure.getMessage(), failure);
        }
        return (String) item;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        quietly(out);
        quietly(in);
        quietly(resource);
    }

    private static void quietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
            // the connection is gone either way
        }
    }
}
