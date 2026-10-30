package dev.vcourt.engine.driver;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Send commands to the driver and return its replies: protocol 1, one JSON line each way.
 *
 * <p>Each command carries the next {@code id}, and its reply must carry the same. A reply
 * that does not come in time, does not parse, answers another id, or a connection that
 * fails or closes, leaves the client <em>broken</em>: a late reply would answer the next
 * command, so every later call fails at once without sending. A reply with
 * {@code ok: false} is a refusal; the connection stays usable.
 *
 * <p>Ported from the desktop's {@code court_booker.driver.DriverClient}, with the same
 * messages and broken-connection rules.
 */
public final class DriverClient implements AutoCloseable {
    /** The protocol this client speaks; {@link #hello} refuses a driver speaking another. */
    public static final int PROTOCOL = 1;
    /** Milliseconds the driver may take to answer a command; a screen read takes about 13. */
    public static final long COMMAND_TIMEOUT_MILLIS = 10_000;

    private final DriverTransport transport;
    private final long timeoutMillis;
    private long lastId;
    private String broken;

    /** Talk over {@code transport}, waiting {@link #COMMAND_TIMEOUT_MILLIS} for each reply. */
    public DriverClient(DriverTransport transport) {
        this(transport, COMMAND_TIMEOUT_MILLIS);
    }

    /** Talk over {@code transport}, waiting {@code timeoutMillis} for each reply. */
    public DriverClient(DriverTransport transport, long timeoutMillis) {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("The timeout must be positive");
        }
        this.transport = transport;
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Return the driver's reply to {@code op} with {@code args}.
     *
     * @throws DriverRefusedException when the driver answers {@code ok: false}
     * @throws DriverNotRespondingException when no reply comes in time
     * @throws DriverException when the connection is or becomes unusable
     */
    public synchronized Map<String, Object> call(String op, Map<String, ?> args)
            throws DriverException {
        if (broken != null) {
            throw new DriverException("The driver connection is lost (" + broken + ")");
        }
        if (args.containsKey("id") || args.containsKey("op")) {
            throw new IllegalArgumentException("id and op are set by the client");
        }
        long id = ++lastId;
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("id", id);
        command.put("op", op);
        command.putAll(args);
        String raw;
        try {
            transport.send(Json.encode(command));
            raw = transport.receive(timeoutMillis);
        } catch (TimeoutException e) {
            broken = op + " timed out";
            throw new DriverNotRespondingException("The driver did not answer " + op, e);
        } catch (IOException e) {
            broken = op + ": " + e.getMessage();
            throw new DriverException(
                    "The driver connection failed during " + op + ": " + e.getMessage(), e);
        }
        if (raw == null) {
            broken = "closed during " + op;
            throw new DriverException("The driver closed the connection during " + op);
        }
        Map<String, Object> reply;
        try {
            reply = asObject(Json.decode(raw));
        } catch (IllegalArgumentException e) {
            broken = "no JSON after " + op;
            throw new DriverException(
                    "The driver answered " + op + " with something else than JSON", e);
        }
        Object replyId = reply.get("id");
        if (!(replyId instanceof Long) || (Long) replyId != id) {
            broken = "reply " + replyId + " to command " + id;
            throw new DriverException("The driver answered another command than " + op);
        }
        if (!Boolean.TRUE.equals(reply.get("ok"))) {
            Object error = reply.get("error");
            throw new DriverRefusedException(op, error == null ? null : error.toString());
        }
        return reply;
    }

    /** Return the driver's reply to {@code op}, which takes no arguments. */
    public Map<String, Object> call(String op) throws DriverException {
        return call(op, Collections.emptyMap());
    }

    /**
     * Greet the driver and check it speaks {@link #PROTOCOL}.
     *
     * @throws DriverException when it speaks another protocol, or as {@link #call}
     */
    public Hello hello() throws DriverException {
        Map<String, Object> reply = call("hello");
        Object protocol = reply.get("protocol");
        Object version = reply.get("version");
        if (!(protocol instanceof Long) || (Long) protocol != PROTOCOL) {
            throw new DriverException("The driver " + version + " speaks protocol " + protocol
                    + ", not " + PROTOCOL);
        }
        return new Hello(PROTOCOL, version == null ? "" : version.toString(),
                (int) integer(reply, "sdk", "hello"));
    }

    /** Return the active window as the XML of a uiautomator2 dump. */
    public String dump() throws DriverException {
        return text(call("dump"), "xml", "dump");
    }

    /** Return the package of the active window, empty when there is none. */
    public String currentPackage() throws DriverException {
        return text(call("current"), "package", "current");
    }

    /**
     * Click the element labelled {@code label} where it is now. {@code (x, y)}, where the
     * last read showed it, only picks among elements with the same label.
     */
    public ClickResult click(String label, int x, int y) throws DriverException {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("label", label);
        args.put("x", x);
        args.put("y", y);
        return clickResult(call("click", args), "click");
    }

    /** Click the smallest clickable element at {@code (x, y)}: an unlabelled one. */
    public ClickResult clickAt(int x, int y) throws DriverException {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("x", x);
        args.put("y", y);
        return clickResult(call("click_at", args), "click_at");
    }

    /** Press Back. */
    public void back() throws DriverException {
        call("back");
    }

    /** Swipe from {@code (x1, y1)} to {@code (x2, y2)} in {@code millis}. */
    public void swipe(int x1, int y1, int x2, int y2, long millis) throws DriverException {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("x1", x1);
        args.put("y1", y1);
        args.put("x2", x2);
        args.put("y2", y2);
        args.put("ms", millis);
        call("swipe", args);
    }

    /** Ask the driver to stop serving; its instrumentation then ends. */
    public void stop() throws DriverException {
        call("stop");
    }

    /** Tell whether a failure made the connection unusable. */
    public synchronized boolean isBroken() {
        return broken != null;
    }

    /** Close the connection; later calls fail without sending. */
    @Override
    public synchronized void close() {
        if (broken == null) {
            broken = "closed";
        }
        transport.close();
    }

    private static ClickResult clickResult(Map<String, Object> reply, String op)
            throws DriverException {
        Object clicked = reply.get("clicked");
        if (!(clicked instanceof Boolean)) {
            throw new DriverException("The driver's reply to " + op + " has no clicked");
        }
        Object how = reply.get("how");
        Object bounds = reply.get("bounds");
        return new ClickResult((Boolean) clicked, how == null ? null : how.toString(),
                bounds == null ? null : bounds.toString());
    }

    private static String text(Map<String, Object> reply, String key, String op)
            throws DriverException {
        Object value = reply.get(key);
        if (!(value instanceof String)) {
            throw new DriverException("The driver's reply to " + op + " has no " + key);
        }
        return (String) value;
    }

    private static long integer(Map<String, Object> reply, String key, String op)
            throws DriverException {
        Object value = reply.get(key);
        if (!(value instanceof Long)) {
            throw new DriverException("The driver's reply to " + op + " has no " + key);
        }
        return (Long) value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObject(Object value) {
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("A reply is a JSON object");
        }
        return (Map<String, Object>) value;
    }
}
