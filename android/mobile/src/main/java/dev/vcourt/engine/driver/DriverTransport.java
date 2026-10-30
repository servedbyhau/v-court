package dev.vcourt.engine.driver;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * One connection to the driver, carrying protocol 1's lines: one JSON object each way per
 * command. The Android session (a {@code LocalSocket} to {@code court-driver}) and the
 * tests' fakes implement it.
 */
public interface DriverTransport extends Closeable {
    /**
     * Send {@code line}, which holds no line break; the transport ends it.
     *
     * @throws IOException when the connection fails
     */
    void send(String line) throws IOException;

    /**
     * Return the next line without its line break, or {@code null} once the driver closed
     * the connection.
     *
     * @throws TimeoutException when no line came within {@code timeoutMillis}
     * @throws IOException when the connection fails
     */
    String receive(long timeoutMillis) throws IOException, TimeoutException;

    /** Close the connection; a call after the first does nothing. */
    @Override
    void close();
}
