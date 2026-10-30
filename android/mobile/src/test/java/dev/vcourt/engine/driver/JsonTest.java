package dev.vcourt.engine.driver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class JsonTest {
    @Test
    void encodesAnObjectInItsOrder() {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("id", 3L);
        command.put("op", "swipe");
        command.put("x1", 270);
        command.put("ok", true);
        command.put("none", null);
        command.put("list", Arrays.asList(1, "a"));

        assertEquals("{\"id\":3,\"op\":\"swipe\",\"x1\":270,\"ok\":true,\"none\":null,"
                + "\"list\":[1,\"a\"]}", Json.encode(command));
    }

    @Test
    void keepsVietnameseAsItIs() {
        assertEquals("\"Sân Tennis\"", Json.encode("Sân Tennis"));
    }

    @Test
    void escapesQuotesBackslashesAndLineBreaks() {
        assertEquals("\"a\\\"b\\\\c\\nd\\re\\tf\\u0001\\u2028\"",
                Json.encode("a\"b\\c\nd\re\tf\u0001\u2028"));
    }

    @Test
    void refusesValuesJsonCannotHold() {
        assertThrows(IllegalArgumentException.class, () -> Json.encode(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> Json.encode(new Object()));
        assertThrows(IllegalArgumentException.class, () -> Json.encode(Map.of(1, "a")));
    }

    @Test
    void decodesADriverReply() {
        Object reply = Json.decode(" {\"id\": 2, \"ok\": true, \"clicked\": true,"
                + " \"how\": \"touch\", \"bounds\": \"[0,0][540,1600]\"} ");

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("id", 2L);
        expected.put("ok", true);
        expected.put("clicked", true);
        expected.put("how", "touch");
        expected.put("bounds", "[0,0][540,1600]");
        assertEquals(expected, reply);
    }

    @Test
    void decodesNestedValues() {
        assertEquals(Map.of("a", List.of(1L, 2.5, -3L, 1.0e3, Map.of(), List.of())),
                Json.decode("{\"a\":[1,2.5,-3,1e3,{},[]]}"));
    }

    @Test
    void decodesEscapes() {
        assertEquals("\"\\/\b\f\n\r\tÂ😀",
                Json.decode("\"\\\"\\\\\\/\\b\\f\\n\\r\\t\\u00c2\\ud83d\\ude00\""));
    }

    @Test
    void keepsAnIntegerTooLargeForALong() {
        assertEquals(1.0e20, Json.decode("100000000000000000000"));
    }

    @Test
    void decodesLiterals() {
        assertEquals(true, Json.decode("true"));
        assertEquals(false, Json.decode("false"));
        assertEquals(null, Json.decode("null"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "{", "{\"a\"}", "{\"a\":1,}", "[1,]", "{a:1}", "01",
            "-", "1.", "1e", "\"open", "\"bad \\x\"", "\"\\u12\"", "\"tab\there\"", "tru",
            "{} {}", "[1] x", "+1"})
    void refusesWhatIsNotOneJsonValue(String text) {
        assertThrows(IllegalArgumentException.class, () -> Json.decode(text));
    }

    @Test
    void roundTripsWhatItEncodes() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("label", "Tiếp tục \"next\"\n");
        value.put("n", -12L);
        value.put("list", List.of(true, "x"));

        assertEquals(value, Json.decode(Json.encode(value)));
    }
}
