package dev.vcourt.engine.driver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DriverClientTest {
    private final FakeTransport transport = new FakeTransport();
    private final DriverClient client = new DriverClient(transport, 250);

    @Test
    void sendsTheIdTheOpAndTheArgumentsInOneLine() throws Exception {
        transport.then("{\"id\":1,\"ok\":true,\"clicked\":true,\"how\":\"action\","
                + "\"bounds\":\"[1,2][3,4]\"}");

        ClickResult result = client.click("S6 - Sân tennis", 270, 800);

        assertEquals(List.of("{\"id\":1,\"op\":\"click\",\"label\":\"S6 - Sân tennis\","
                + "\"x\":270,\"y\":800}"), transport.sent);
        assertTrue(result.clicked());
        assertEquals("action", result.how());
        assertEquals("[1,2][3,4]", result.bounds());
        assertEquals(List.of(250L), transport.timeouts);
    }

    @Test
    void numbersEachCommandOnFromOne() throws Exception {
        transport.then("{\"id\":1,\"ok\":true}").then("{\"id\":2,\"ok\":true}");

        client.back();
        client.back();

        assertEquals(List.of("{\"id\":1,\"op\":\"back\"}", "{\"id\":2,\"op\":\"back\"}"),
                transport.sent);
    }

    @Test
    void aRefusalLeavesTheConnectionUsable() throws Exception {
        transport.then("{\"id\":1,\"ok\":false,\"error\":\"unknown op: zoom\"}")
                .then("{\"id\":2,\"ok\":true,\"xml\":\"<hierarchy/>\"}");

        DriverRefusedException refused =
                assertThrows(DriverRefusedException.class, () -> client.call("zoom"));

        assertEquals("The driver refused zoom: unknown op: zoom", refused.getMessage());
        assertEquals("zoom", refused.op());
        assertEquals("unknown op: zoom", refused.reason());
        assertFalse(client.isBroken());
        assertEquals("<hierarchy/>", client.dump());
    }

    @Test
    void aRefusalWithoutAnErrorHasNoReason() {
        transport.then("{\"id\":1,\"ok\":false}");

        DriverRefusedException refused =
                assertThrows(DriverRefusedException.class, () -> client.back());

        assertNull(refused.reason());
    }

    @Test
    void aTimeoutLosesTheConnection() {
        transport.then(FakeTransport.TIMEOUT);

        DriverNotRespondingException late =
                assertThrows(DriverNotRespondingException.class, () -> client.dump());
        DriverException after = assertThrows(DriverException.class, () -> client.dump());

        assertEquals("The driver did not answer dump", late.getMessage());
        // A late reply would answer the next command, so nothing more is sent.
        assertEquals("The driver connection is lost (dump timed out)", after.getMessage());
        assertEquals(1, transport.sent.size());
        assertTrue(client.isBroken());
    }

    @Test
    void theEndOfTheConnectionLosesIt() {
        transport.then(FakeTransport.EOF);

        DriverException closed = assertThrows(DriverException.class, () -> client.back());

        assertEquals("The driver closed the connection during back", closed.getMessage());
        assertBroken("closed during back");
    }

    @Test
    void aFailingConnectionIsLost() {
        transport.then(new IOException("Broken pipe"));

        DriverException failed = assertThrows(DriverException.class, () -> client.back());

        assertEquals("The driver connection failed during back: Broken pipe", failed.getMessage());
        assertBroken("back: Broken pipe");
    }

    @Test
    void aReplyThatIsNotJsonLosesTheConnection() {
        transport.then("Driver crashed");

        DriverException garbled = assertThrows(DriverException.class, () -> client.dump());

        assertEquals("The driver answered dump with something else than JSON",
                garbled.getMessage());
        assertBroken("no JSON after dump");
    }

    @Test
    void aReplyThatIsNotAnObjectLosesTheConnection() {
        transport.then("[1,true]");

        assertThrows(DriverException.class, () -> client.dump());

        assertBroken("no JSON after dump");
    }

    @Test
    void aReplyToAnotherCommandLosesTheConnection() {
        transport.then("{\"id\":7,\"ok\":true}");

        DriverException other = assertThrows(DriverException.class, () -> client.back());

        assertEquals("The driver answered another command than back", other.getMessage());
        assertBroken("reply 7 to command 1");
    }

    @Test
    void aReplyWithoutAnIdLosesTheConnection() {
        transport.then("{\"ok\":true}");

        assertThrows(DriverException.class, () -> client.back());

        assertBroken("reply null to command 1");
    }

    @Test
    void helloAcceptsProtocolOne() throws Exception {
        transport.then("{\"id\":1,\"ok\":true,\"protocol\":1,\"version\":\"a1b2c3d4e5f6\","
                + "\"sdk\":35}");

        Hello hello = client.hello();

        assertEquals(1, hello.protocol());
        assertEquals("a1b2c3d4e5f6", hello.version());
        assertEquals(35, hello.sdk());
    }

    @Test
    void helloRefusesAnotherProtocol() {
        transport.then("{\"id\":1,\"ok\":true,\"protocol\":2,\"version\":\"new\",\"sdk\":35}");

        DriverException other = assertThrows(DriverException.class, () -> client.hello());

        assertEquals("The driver new speaks protocol 2, not 1", other.getMessage());
    }

    @Test
    void aReplyMissingItsValueIsAnErrorButNotALoss() {
        transport.then("{\"id\":1,\"ok\":true}").then("{\"id\":2,\"ok\":true,\"package\":\"\"}");

        DriverException missing = assertThrows(DriverException.class, () -> client.dump());

        assertEquals("The driver's reply to dump has no xml", missing.getMessage());
        assertFalse(client.isBroken());
    }

    @Test
    void currentPackageIsEmptyWithoutAWindow() throws Exception {
        transport.then("{\"id\":1,\"ok\":true,\"package\":\"\"}");

        assertEquals("", client.currentPackage());
    }

    @Test
    void clickAtSendsThePointAlone() throws Exception {
        transport.then("{\"id\":1,\"ok\":true,\"clicked\":false}");

        ClickResult result = client.clickAt(480, 1290);

        assertEquals(List.of("{\"id\":1,\"op\":\"click_at\",\"x\":480,\"y\":1290}"),
                transport.sent);
        assertFalse(result.clicked());
        assertNull(result.how());
        assertNull(result.bounds());
    }

    @Test
    void swipeSendsItsPointsAndDuration() throws Exception {
        transport.then("{\"id\":1,\"ok\":true}");

        client.swipe(270, 1200, 270, 600, 250);

        assertEquals(List.of("{\"id\":1,\"op\":\"swipe\",\"x1\":270,\"y1\":1200,\"x2\":270,"
                + "\"y2\":600,\"ms\":250}"), transport.sent);
    }

    @Test
    void stopIsACommand() throws Exception {
        transport.then("{\"id\":1,\"ok\":true}");

        client.stop();

        assertEquals(List.of("{\"id\":1,\"op\":\"stop\"}"), transport.sent);
    }

    @Test
    void theClientSetsIdAndOp() {
        assertThrows(IllegalArgumentException.class,
                () -> client.call("dump", Map.of("id", 9)));
        assertThrows(IllegalArgumentException.class,
                () -> client.call("dump", Collections.singletonMap("op", "back")));
        assertTrue(transport.sent.isEmpty());
    }

    @Test
    void closingEndsTheTransportAndLaterCalls() {
        client.close();

        DriverException closed = assertThrows(DriverException.class, () -> client.back());

        assertTrue(transport.closed);
        assertEquals("The driver connection is lost (closed)", closed.getMessage());
    }

    @Test
    void theTimeoutMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new DriverClient(transport, 0));
    }

    private void assertBroken(String reason) {
        assertTrue(client.isBroken());
        DriverException after = assertThrows(DriverException.class, () -> client.back());
        assertEquals("The driver connection is lost (" + reason + ")", after.getMessage());
    }
}
