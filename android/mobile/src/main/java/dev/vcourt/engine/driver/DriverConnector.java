package dev.vcourt.engine.driver;

import java.io.IOException;

/** Opens connections to the driver's socket: on Android, a {@code LocalSocket}. */
public interface DriverConnector {
    /**
     * Connect to the driver's socket.
     *
     * @throws IOException when nothing serves it
     */
    DriverTransport connect() throws IOException;
}
