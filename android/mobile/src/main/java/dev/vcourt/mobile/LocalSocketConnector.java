package dev.vcourt.mobile;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import java.io.IOException;

import dev.vcourt.engine.driver.DriverConnector;
import dev.vcourt.engine.driver.DriverTransport;
import dev.vcourt.engine.driver.StreamTransport;

/** Connects to the driver's abstract socket {@value DriverContract#SOCKET}. */
final class LocalSocketConnector implements DriverConnector {
    @Override
    public DriverTransport connect() throws IOException {
        LocalSocket socket = new LocalSocket();
        try {
            socket.connect(new LocalSocketAddress(DriverContract.SOCKET));
        } catch (IOException e) {
            socket.close();
            throw e;
        }
        return new StreamTransport(socket.getInputStream(), socket.getOutputStream(), socket);
    }
}
