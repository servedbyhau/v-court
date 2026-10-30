package dev.vcourt.engine.driver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

class StreamTransportTest {
    @Test
    void endsEachLineItSends() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (StreamTransport transport = new StreamTransport(empty(), output, null)) {
            transport.send("{\"op\":\"Sân\"}");
        }

        assertEquals("{\"op\":\"Sân\"}\n", output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void refusesALineWithABreak() {
        try (StreamTransport transport =
                new StreamTransport(empty(), new ByteArrayOutputStream(), null)) {
            assertThrows(IllegalArgumentException.class, () -> transport.send("a\nb"));
            assertThrows(IllegalArgumentException.class, () -> transport.send("a\rb"));
        }
    }

    @Test
    void receivesLinesThenTheEndForEveryLaterRead() throws Exception {
        InputStream input = new ByteArrayInputStream(
                "first\r\nsecond\n".getBytes(StandardCharsets.UTF_8));
        try (StreamTransport transport =
                new StreamTransport(input, new ByteArrayOutputStream(), null)) {
            assertEquals("first", transport.receive(1000));
            assertEquals("second", transport.receive(1000));
            assertNull(transport.receive(1000));
            assertNull(transport.receive(1000));
        }
    }

    @Test
    void timesOutWhenNoLineComes() throws Exception {
        try (ServerSocket server = loopback(); Socket socket = connect(server);
                Socket ignored = server.accept();
                StreamTransport transport = transport(socket)) {
            long start = System.nanoTime();

            assertThrows(TimeoutException.class, () -> transport.receive(100));

            assertTrue(System.nanoTime() - start >= 90_000_000L);
        }
    }

    @Test
    void reportsAFailedRead() throws Exception {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Connection reset");
            }
        };
        try (StreamTransport transport =
                new StreamTransport(failing, new ByteArrayOutputStream(), null)) {
            IOException failure =
                    assertThrows(IOException.class, () -> transport.receive(1000));

            assertEquals("Connection reset", failure.getMessage());
            assertNull(transport.receive(1000));
        }
    }

    @Test
    void closingClosesTheResourceAndRefusesSends() throws Exception {
        boolean[] closed = {false};
        StreamTransport transport =
                new StreamTransport(empty(), new ByteArrayOutputStream(), () -> closed[0] = true);

        transport.close();
        transport.close();

        assertTrue(closed[0]);
        assertThrows(IOException.class, () -> transport.send("{}"));
    }

    /**
     * Closing returns at once while the reader waits on a connection the peer keeps open: the
     * driver inside V Court (started with --no-restart) never closes its end, and closing the
     * reader first waited for that read forever (MuMu, 2026-10-30). The input stands for an
     * Android {@code LocalSocket}: closing its stream does not wake a blocked read; only the
     * socket's shutdown, the resource, does.
     */
    @Test
    void closingReturnsWhileThePeerKeepsTheConnectionOpen() throws Exception {
        CountDownLatch shutdown = new CountDownLatch(1);
        InputStream socketInput = new InputStream() {
            @Override
            public int read() throws IOException {
                try {
                    shutdown.await();
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
                return -1;
            }
        };
        StreamTransport transport = new StreamTransport(socketInput, new ByteArrayOutputStream(),
                shutdown::countDown);
        Thread.sleep(100); // the reader thread is now blocked in its read

        assertTimeoutPreemptively(Duration.ofSeconds(2), transport::close);

        assertEquals(0, shutdown.getCount());
        assertNull(transport.receive(1000));
    }

    /** The client over a real socket against a stand-in driver speaking protocol 1. */
    @Test
    void carriesAClientSessionOverASocket() throws Exception {
        try (ServerSocket server = loopback()) {
            Thread driver = new Thread(() -> serveFakeDriver(server), "fake-driver");
            driver.setDaemon(true);
            driver.start();
            try (DriverClient client = new DriverClient(transport(connect(server)), 2000)) {
                Hello hello = client.hello();
                String xml = client.dump();
                ClickResult result = client.click("Tiếp tục", 270, 1500);
                DriverRefusedException refused =
                        assertThrows(DriverRefusedException.class, () -> client.call("zoom"));
                client.stop();

                assertEquals("test", hello.version());
                assertEquals("<hierarchy><node content-desc=\"Tiện ích\"/></hierarchy>", xml);
                assertTrue(result.clicked());
                assertEquals("label Tiếp tục at 270,1500", result.bounds());
                assertEquals("unknown op: zoom", refused.reason());
                DriverException closed =
                        assertThrows(DriverException.class, () -> client.back());
                assertEquals("The driver closed the connection during back",
                        closed.getMessage());
            }
            driver.join(2000);
        }
    }

    /** Answer like {@code Driver.answer} until {@code stop}, then close the connection. */
    private static void serveFakeDriver(ServerSocket server) {
        try (Socket socket = server.accept();
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                Writer out = new OutputStreamWriter(
                        socket.getOutputStream(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = in.readLine()) != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> command = (Map<String, Object>) Json.decode(line);
                Object id = command.get("id");
                String reply;
                switch ((String) command.get("op")) {
                    case "hello":
                        reply = "\"protocol\":1,\"version\":\"test\",\"sdk\":35";
                        break;
                    case "dump":
                        reply = "\"xml\":" + Json.encode(
                                "<hierarchy><node content-desc=\"Tiện ích\"/></hierarchy>");
                        break;
                    case "click":
                        reply = "\"clicked\":true,\"how\":\"action\",\"bounds\":" + Json.encode(
                                "label " + command.get("label") + " at " + command.get("x")
                                        + "," + command.get("y"));
                        break;
                    case "stop":
                        out.write("{\"id\":" + id + ",\"ok\":true}\n");
                        out.flush();
                        return;
                    default:
                        out.write("{\"id\":" + id + ",\"ok\":false,\"error\":\"unknown op: "
                                + command.get("op") + "\"}\n");
                        out.flush();
                        continue;
                }
                out.write("{\"id\":" + id + ",\"ok\":true," + reply + "}\n");
                out.flush();
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static ServerSocket loopback() throws IOException {
        return new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    }

    private static Socket connect(ServerSocket server) throws IOException {
        return new Socket(InetAddress.getLoopbackAddress(), server.getLocalPort());
    }

    private static StreamTransport transport(Socket socket) throws IOException {
        InputStream input = socket.getInputStream();
        OutputStream output = socket.getOutputStream();
        return new StreamTransport(input, output, socket);
    }

    private static InputStream empty() {
        return new ByteArrayInputStream(new byte[0]);
    }
}
