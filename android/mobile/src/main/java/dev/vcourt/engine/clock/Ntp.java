package dev.vcourt.engine.clock;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/** Asks time servers for the clock's offset; ported from the desktop's {@code clock.py}. */
public final class Ntp {
    private static final Logger LOG = Logger.getLogger(Ntp.class.getName());

    /** Servers asked in turn when the configured one does not answer. */
    public static final List<String> FALLBACKS =
            List.of("time.google.com", "time.windows.com", "pool.ntp.org");
    static final int PORT = 123;
    static final int TIMEOUT_MS = 1000;
    static final int ATTEMPTS = 2;
    /** Answers asked of a server per check; the one with the shortest round trip is kept. */
    static final int SAMPLES = 4;
    static final long EPOCH_OFFSET = 2_208_988_800L; // seconds from 1900 to 1970
    static final int PACKET_SIZE = 48;
    static final int SERVER_MODE = 4;
    static final int MAX_STRATUM = 15;

    /** One answer of a time server. */
    public static final class Sample {
        /** Seconds to add to the local clock to match the server's. */
        public final double offset;
        /** Round trip less the server's own time; the offset is off by half of it at most. */
        public final double delay;

        public Sample(double offset, double delay) {
            this.offset = offset;
            this.delay = delay;
        }
    }

    /** Asks one server once; null when it does not answer usefully. */
    public interface Query {
        Sample ask(String server);
    }

    private Ntp() {
    }

    /**
     * Ask {@code server} once over UDP. The name is resolved before the exchange is timed: a
     * DNS lookup inside it would shift the offset by half of it.
     */
    public static Sample query(String server) {
        byte[] request = new byte[PACKET_SIZE];
        request[0] = 0x1b; // version 3, client mode
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(server);
            socket.setSoTimeout(TIMEOUT_MS);
            byte[] reply = new byte[PACKET_SIZE];
            double sent = System.currentTimeMillis() / 1000.0;
            socket.send(new DatagramPacket(request, request.length, address, PORT));
            DatagramPacket packet = new DatagramPacket(reply, reply.length);
            socket.receive(packet);
            double received = System.currentTimeMillis() / 1000.0;
            Sample sample = readReply(packet.getLength() < PACKET_SIZE ? new byte[0] : reply,
                    sent, received);
            if (sample == null) {
                LOG.info("Invalid reply from " + server);
            }
            return sample;
        } catch (IOException | SecurityException e) {
            LOG.info("Clock check against " + server + " failed (" + e + ")");
            return null;
        }
    }

    /**
     * Return the answer in an NTP reply given the local send and receive times; null for a
     * short reply, one not from a server, or from an unsynchronised server (stratum 0).
     */
    public static Sample readReply(byte[] reply, double sent, double received) {
        if (reply.length < PACKET_SIZE) {
            return null;
        }
        int stratum = reply[1] & 0xff;
        if ((reply[0] & 0x07) != SERVER_MODE || stratum < 1 || stratum > MAX_STRATUM) {
            return null;
        }
        double serverReceived = timestamp(reply, 32);
        double serverSent = timestamp(reply, 40);
        return new Sample(((serverReceived - sent) + (serverSent - received)) / 2,
                (received - sent) - (serverSent - serverReceived));
    }

    /**
     * Return seconds to add to the local clock to match NTP time, or null. Asks
     * {@code server}, then each fallback not asked yet, and stops at the first that answers:
     * {@link #SAMPLES} times or until {@link #ATTEMPTS} questions failed, keeping the answer
     * with the shortest round trip.
     */
    public static Double offset(String server, Query query) {
        Set<String> servers = new LinkedHashSet<>();
        servers.add(server);
        servers.addAll(FALLBACKS);
        for (String name : servers) {
            List<Sample> answers = new ArrayList<>();
            int failed = 0;
            while (answers.size() < SAMPLES && failed < ATTEMPTS) {
                Sample sample = query.ask(name);
                if (sample == null) {
                    failed++;
                } else {
                    answers.add(sample);
                }
            }
            if (!answers.isEmpty()) {
                Sample best = answers.get(0);
                for (Sample sample : answers) {
                    if (sample.delay < best.delay) {
                        best = sample;
                    }
                }
                LOG.info("Clock checked against " + name + ": round trip "
                        + Math.round(best.delay * 1000) + " ms (best of " + answers.size() + ")");
                return best.offset;
            }
        }
        LOG.warning("No time server answered (" + String.join(", ", servers) + ")");
        return null;
    }

    static double timestamp(byte[] packet, int position) {
        ByteBuffer buffer = ByteBuffer.wrap(packet, position, 8);
        long seconds = buffer.getInt() & 0xffffffffL;
        long fraction = buffer.getInt() & 0xffffffffL;
        return seconds - EPOCH_OFFSET + fraction / 4294967296.0;
    }
}
