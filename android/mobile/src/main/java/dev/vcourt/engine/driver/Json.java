package dev.vcourt.engine.driver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The JSON of driver protocol 1, without Android's {@code org.json}, so the engine runs on
 * any JVM and its tests need no device.
 *
 * <p>Objects decode to {@link LinkedHashMap}, arrays to {@link ArrayList}, integers to
 * {@link Long}, other numbers to {@link Double}. Text is encoded as it is, not escaped to
 * ASCII: the socket carries UTF-8, and court names are Vietnamese.
 */
final class Json {
    private Json() {}

    /** Return {@code value} (a map, list, string, number, boolean or null) as JSON text. */
    static String encode(Object value) {
        StringBuilder out = new StringBuilder();
        write(out, value);
        return out.toString();
    }

    /**
     * Return the value of the JSON text {@code text}.
     *
     * @throws IllegalArgumentException when {@code text} is not one complete JSON value
     */
    static Object decode(String text) {
        Parser parser = new Parser(text);
        parser.skipSpace();
        Object value = parser.value();
        parser.skipSpace();
        if (parser.at < text.length()) {
            throw parser.error("text after the value");
        }
        return value;
    }

    private static void write(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String) {
            writeString(out, (String) value);
        } else if (value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Long || value instanceof Integer
                || value instanceof Short || value instanceof Byte) {
            out.append(((Number) value).longValue());
        } else if (value instanceof Double || value instanceof Float) {
            double number = ((Number) value).doubleValue();
            if (Double.isNaN(number) || Double.isInfinite(number)) {
                throw new IllegalArgumentException("JSON has no " + number);
            }
            out.append(number);
        } else if (value instanceof Map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    throw new IllegalArgumentException("JSON keys are strings: " + entry.getKey());
                }
                if (!first) {
                    out.append(',');
                }
                first = false;
                writeString(out, (String) entry.getKey());
                out.append(':');
                write(out, entry.getValue());
            }
            out.append('}');
        } else if (value instanceof List) {
            out.append('[');
            boolean first = true;
            for (Object item : (List<?>) value) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                write(out, item);
            }
            out.append(']');
        } else {
            throw new IllegalArgumentException("Not a JSON value: " + value.getClass().getName());
        }
    }

    private static void writeString(StringBuilder out, String text) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    // Line separators too: a raw U+2028 would end the line for some readers.
                    if (c < 0x20 || c == ' ' || c == ' ') {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
    }

    private static final class Parser {
        private final String text;
        private int at;

        Parser(String text) {
            this.text = text;
        }

        Object value() {
            if (at >= text.length()) {
                throw error("end of text where a value was expected");
            }
            char c = text.charAt(at);
            switch (c) {
                case '{':
                    return object();
                case '[':
                    return array();
                case '"':
                    return string();
                case 't':
                    return word("true", Boolean.TRUE);
                case 'f':
                    return word("false", Boolean.FALSE);
                case 'n':
                    return word("null", null);
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        return number();
                    }
                    throw error("unexpected '" + c + "'");
            }
        }

        private Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<>();
            at++;
            skipSpace();
            if (peek() == '}') {
                at++;
                return map;
            }
            while (true) {
                skipSpace();
                if (peek() != '"') {
                    throw error("a key must be a string");
                }
                String key = string();
                skipSpace();
                expect(':');
                skipSpace();
                map.put(key, value());
                skipSpace();
                if (peek() == ',') {
                    at++;
                } else {
                    expect('}');
                    return map;
                }
            }
        }

        private List<Object> array() {
            List<Object> list = new ArrayList<>();
            at++;
            skipSpace();
            if (peek() == ']') {
                at++;
                return list;
            }
            while (true) {
                skipSpace();
                list.add(value());
                skipSpace();
                if (peek() == ',') {
                    at++;
                } else {
                    expect(']');
                    return list;
                }
            }
        }

        private String string() {
            StringBuilder out = new StringBuilder();
            at++;
            while (true) {
                if (at >= text.length()) {
                    throw error("unterminated string");
                }
                char c = text.charAt(at++);
                if (c == '"') {
                    return out.toString();
                }
                if (c < 0x20) {
                    throw error("control character in a string");
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (at >= text.length()) {
                    throw error("unterminated escape");
                }
                char escaped = text.charAt(at++);
                switch (escaped) {
                    case '"':
                    case '\\':
                    case '/':
                        out.append(escaped);
                        break;
                    case 'b':
                        out.append('\b');
                        break;
                    case 'f':
                        out.append('\f');
                        break;
                    case 'n':
                        out.append('\n');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    case 't':
                        out.append('\t');
                        break;
                    case 'u':
                        if (at + 4 > text.length()) {
                            throw error("short \\u escape");
                        }
                        try {
                            out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
                        } catch (NumberFormatException e) {
                            throw error("bad \\u escape");
                        }
                        at += 4;
                        break;
                    default:
                        throw error("bad escape \\" + escaped);
                }
            }
        }

        private Object number() {
            int start = at;
            if (peek() == '-') {
                at++;
            }
            if (peek() == '0') {
                at++;
            } else if (isDigit(peek())) {
                digits();
            } else {
                throw error("a number needs digits");
            }
            boolean integer = true;
            if (peek() == '.') {
                integer = false;
                at++;
                if (!isDigit(peek())) {
                    throw error("digits must follow '.'");
                }
                digits();
            }
            if (peek() == 'e' || peek() == 'E') {
                integer = false;
                at++;
                if (peek() == '+' || peek() == '-') {
                    at++;
                }
                if (!isDigit(peek())) {
                    throw error("an exponent needs digits");
                }
                digits();
            }
            String number = text.substring(start, at);
            if (integer) {
                try {
                    return Long.parseLong(number);
                } catch (NumberFormatException tooLarge) {
                    return Double.parseDouble(number);
                }
            }
            return Double.parseDouble(number);
        }

        private void digits() {
            while (isDigit(peek())) {
                at++;
            }
        }

        private Object word(String word, Object value) {
            if (!text.startsWith(word, at)) {
                throw error("unexpected '" + text.charAt(at) + "'");
            }
            at += word.length();
            return value;
        }

        void skipSpace() {
            while (at < text.length()) {
                char c = text.charAt(at);
                if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                    return;
                }
                at++;
            }
        }

        private void expect(char c) {
            if (peek() != c) {
                throw error("expected '" + c + "'");
            }
            at++;
        }

        private char peek() {
            return at < text.length() ? text.charAt(at) : '\0';
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        IllegalArgumentException error(String problem) {
            return new IllegalArgumentException("Bad JSON at " + at + ": " + problem);
        }
    }
}
