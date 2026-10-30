package dev.vcourt.engine.flow;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What the app showed for a round that went through, read from screens the round reads
 * anyway, so it costs no device command. A detail not found stays null; so does the
 * ticket's in a dry run.
 */
public final class Booked {
    /** ISO date on the confirmation page. */
    public String shownDate;
    /** Slot on the confirmation page. */
    public String shownSlot;
    /** Court on the confirmation page: the one booked, which once differed from the tap. */
    public String shownCourt;
    /** The ticket's status in the read that showed the ticket, if it showed already. */
    public String ticketStatus;

    /** Return the details found, as the {@code slot_booked} event's data. */
    public Map<String, Object> eventData() {
        Map<String, Object> data = new LinkedHashMap<>();
        put(data, "shown_date", shownDate);
        put(data, "shown_slot", shownSlot);
        put(data, "shown_court", shownCourt);
        put(data, "ticket_status", ticketStatus);
        return data;
    }

    private static void put(Map<String, Object> data, String key, String value) {
        if (value != null) {
            data.put(key, value);
        }
    }
}
