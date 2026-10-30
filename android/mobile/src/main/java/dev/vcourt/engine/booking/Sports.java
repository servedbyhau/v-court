package dev.vcourt.engine.booking;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The sports V Court books, by the desktop's keys, each with its tile on the utilities list
 * in Vietnamese ({@code config.SPORTS}) and English ({@code app_language.ENGLISH_SPORTS}).
 */
public final class Sports {
    /** Vietnamese tiles by sport key, in the desktop's order. */
    public static final Map<String, String> VIETNAMESE;
    /** English tiles by sport key, read on 2026-10-27. */
    public static final Map<String, String> ENGLISH;

    static {
        Map<String, String> vi = new LinkedHashMap<>();
        vi.put("tennis", "Sân Tennis");
        vi.put("multi", "Sân đa năng");
        vi.put("volleyball", "Sân bóng chuyền");
        vi.put("table-tennis", "Sân bóng bàn");
        vi.put("basketball", "Sân bóng rổ");
        vi.put("football", "Sân bóng đá");
        vi.put("pickleball", "Sân Pickleball");
        vi.put("badminton", "Sân cầu lông");
        VIETNAMESE = Collections.unmodifiableMap(vi);
        Map<String, String> en = new LinkedHashMap<>();
        en.put("tennis", "Tennis");
        en.put("multi", "Multi Purpose Yard");
        en.put("volleyball", "Volleyball");
        en.put("table-tennis", "Pingpong");
        en.put("basketball", "Basketball");
        en.put("football", "Football");
        en.put("pickleball", "Pickleball");
        en.put("badminton", "Badminton");
        ENGLISH = Collections.unmodifiableMap(en);
    }

    private Sports() {
    }

    /** Tell whether {@code key} names a sport. */
    public static boolean isKnown(String key) {
        return VIETNAMESE.containsKey(key);
    }

    /** Return the keys joined by commas, for messages. */
    public static String keys() {
        return String.join(", ", VIETNAMESE.keySet());
    }
}
