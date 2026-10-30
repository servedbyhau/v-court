package dev.vcourt.engine.booking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The subdivisions of Vinhomes Grand Park, and the sports, venues and courts of each. Each
 * subdivision's account sees its own utilities, venues and courts; the run needs no
 * subdivision to book, since the court names its venue. Ported from the desktop's
 * {@code catalog.py}; its data is generated from that module, so keep the two equal.
 */
public final class Catalog {
    /** An apartment code as the home screen shows it, such as {@code S1.0101.01}. */
    public static final Pattern APARTMENT = Pattern.compile("[A-Z]{1,4}\\d+(?:\\.\\d+)+");
    /**
     * Kinds of court the names tell: "tập ném" (shooting practice), "nửa sân" (half court),
     * "tập" (practice), "sân 5" and "sân 7" (5- and 7-a-side football), "đa năng"
     * (multi-use).
     */
    public static final List<String> COURT_KINDS =
            List.of("shooting", "half", "practice", "five", "seven", "multi");

    /**
     * A court as the app names it on its court list, and what tells it apart from the other
     * courts of its sport. The app keeps court names in Vietnamese in both languages.
     */
    public static final class Court {
        public final String name;
        /** The block or area the app puts first, such as {@code S6} or {@code BCX3}. */
        public final String code;
        /** One of {@link #COURT_KINDS}, or empty for a plain court. */
        public final String kind;
        /** The court's number among those of its kind, such as {@code 01}; may be empty. */
        public final String number;

        Court(String name, String code, String kind, String number) {
            this.name = name;
            this.code = code;
            this.kind = kind;
            this.number = number;
        }
    }

    /** A venue as the app names it below "Chọn phân khu", in both of its languages. */
    public static final class Venue {
        public final String name;
        public final List<Court> courts;

        Venue(String name, List<Court> courts) {
            this.name = name;
            this.courts = List.copyOf(courts);
        }
    }

    /** A sport of a subdivision, keyed as {@link Sports} keys it. */
    public static final class Sport {
        public final String id;
        public final List<Venue> venues;

        Sport(String id, List<Venue> venues) {
            this.id = id;
            this.venues = List.copyOf(venues);
        }

        /** Return the courts of every venue, in the order to offer them. */
        public List<Court> courts() {
            List<Court> all = new ArrayList<>();
            for (Venue venue : venues) {
                all.addAll(venue.courts);
            }
            return all;
        }
    }

    /** A subdivision, with the sports its accounts see, in the order of the app's list. */
    public static final class Subdivision {
        public final String id;
        public final String name;
        public final List<Sport> sports;

        Subdivision(String id, String name, List<Sport> sports) {
            this.id = id;
            this.name = name;
            this.sports = List.copyOf(sports);
        }

        /** Return the sport keyed {@code sportId}, or null if this subdivision lacks it. */
        public Sport sport(String sportId) {
            for (Sport sport : sports) {
                if (sport.id.equals(sportId)) {
                    return sport;
                }
            }
            return null;
        }
    }

    /** The subdivisions surveyed (2026-10-27), Origami first: the default for other courts. */
    public static final List<Subdivision> SUBDIVISIONS = List.of(
        new Subdivision("origami", "Origami", List.of(
                sport("tennis",
                    venue("Origami_ Sân tennis",
                        court("S6 - Sân tennis", "S6"),
                        court("S8 - Sân tennis", "S8"),
                        court("S9 - Sân tennis", "S9"),
                        court("S10 - Sân tennis", "S10"))),
                sport("multi",
                    venue("Origami _ Sân đa năng",
                        court("S7 - Sân đa năng", "S7"),
                        court("S9 - Sân đa năng", "S9"),
                        court("S10 - Sân đa năng", "S10"))),
                sport("volleyball",
                    venue("Origami _ Sân bóng chuyền hơi",
                        court("S8 - Sân bóng chuyền hơi", "S8"),
                        court("S9 - Sân bóng chuyền hơi", "S9"),
                        court("S10 - Sân bóng chuyền hơi 01", "S10", "", "01"),
                        court("S10 - Sân bóng chuyền hơi 02", "S10", "", "02"))),
                sport("table-tennis",
                    venue("Sân bóng bàn",
                        court("S5 - Sân tập bóng bàn 03", "S5"))),
                sport("basketball",
                    venue("Origami _ Sân bóng rổ",
                        court("S6 - Sân bóng rổ tập ném 01", "S6", "shooting", "01"),
                        court("S6 - Sân bóng rổ tập ném 02", "S6", "shooting", "02"),
                        court("S8 - Sân bóng rổ nửa sân", "S8", "half", ""),
                        court("S9 - Sân bóng rổ tập ném 01", "S9", "shooting", "01"),
                        court("S9 - Sân bóng rổ tập ném 02", "S9", "shooting", "02"),
                        court("S10 - Sân bóng rổ nửa sân 03", "S10", "half", "03"),
                        court("S10 - Sân tập bóng rổ 01", "S10", "practice", "01"))),
                sport("football",
                    venue("Sân bóng đá",
                        court("S5 - Sân bóng đá 5", "S5", "five", ""),
                        court("S5 - Sân bóng đá 7", "S5", "seven", ""))),
                sport("pickleball",
                    venue("Origami _ Sân Pickleball",
                        court("S9 - Sân Pickleball", "S9"))),
                sport("badminton",
                    venue("Origami _ Sân cầu lông",
                        court("S6 - Sân cầu lông", "S6"),
                        court("S8 - Sân Cầu lông", "S8"),
                        court("S10 - Sân cầu lông", "S10"))))),
        new Subdivision("bs", "BS", List.of(
                sport("volleyball",
                    venue("BS _ Sân bóng chuyền hơi",
                        court("BCX3 - Sân bóng chuyền hơi 01", "BCX3", "", "01"),
                        court("BCX3 - Sân bóng chuyền hơi 02", "BCX3", "", "02"))),
                sport("tennis",
                    venue("BE _ Sân tennis",
                        court("BE7 - Sân tennis", "BE7")),
                    venue("BS _ Sân tennis",
                        court("BCX3 - Sân tennis 01", "BCX3", "", "01"),
                        court("BCX3 - Sân tennis 02", "BCX3", "", "02"),
                        court("CX2 - Sân tennis 01", "CX2", "", "01"),
                        court("CX2 - Sân tennis 02", "CX2", "", "02"))),
                sport("basketball",
                    venue("BE _ Sân bóng rổ",
                        court("BE7 - Sân bóng rổ tập ném 01", "BE7", "shooting", "01"),
                        court("BE7 - Sân bóng rổ tập ném 02", "BE7", "shooting", "02")),
                    venue("BS _ Sân bóng rổ",
                        court("BCX3 - Sân bóng rổ tập ném 01", "BCX3", "shooting", "01"),
                        court("BCX3 - Sân bóng rổ tập ném 02", "BCX3", "shooting", "02"))),
                sport("football",
                    venue("BS _ Sân bóng đá",
                        court("BCX3 - Sân bóng đá", "BCX3"))),
                sport("pickleball",
                    venue("BE _ Sân Pickleball",
                        court("BE7 - Sân Pickleball", "BE7")),
                    venue("BS _ Sân Pickleball",
                        court("BCX3 - Sân đa năng (Cầu lông-Pickleball)", "BCX3", "multi", ""))),
                sport("badminton",
                    venue("BS _ Sân cầu lông",
                        court("BCX3 - Sân cầu lông 01", "BCX3")))))
    );

    private static final Map<String, String> VENUES = new LinkedHashMap<>();

    static {
        for (Subdivision subdivision : SUBDIVISIONS) {
            for (Sport sport : subdivision.sports) {
                for (Venue venue : sport.venues) {
                    for (Court court : venue.courts) {
                        VENUES.put(court.name, venue.name);
                    }
                }
            }
        }
    }

    private Catalog() {
    }

    private static Sport sport(String id, Venue... venues) {
        return new Sport(id, List.of(venues));
    }

    private static Venue venue(String name, Court... courts) {
        return new Venue(name, List.of(courts));
    }

    private static Court court(String name, String code) {
        return new Court(name, code, "", "");
    }

    private static Court court(String name, String code, String kind, String number) {
        return new Court(name, code, kind, number);
    }

    /** Return the subdivision keyed {@code id}, or null. */
    public static Subdivision subdivision(String id) {
        for (Subdivision item : SUBDIVISIONS) {
            if (item.id.equals(id)) {
                return item;
            }
        }
        return null;
    }

    /** Return the name of the venue that lists {@code court}, or null for one not here. */
    public static String venueOf(String court) {
        return VENUES.get(court);
    }

    /** Return the ids of the subdivisions that list {@code court}; empty for one not here. */
    public static List<String> subdivisionsOf(String court) {
        List<String> found = new ArrayList<>();
        for (Subdivision item : SUBDIVISIONS) {
            boolean lists = false;
            for (Sport sport : item.sports) {
                for (Court c : sport.courts()) {
                    lists |= c.name.equals(court);
                }
            }
            if (lists) {
                found.add(item.id);
            }
        }
        return found;
    }

    /** Tell whether the catalog lists {@code court} in other subdivisions only. */
    public static boolean courtOutside(String subdivisionId, String court) {
        List<String> found = subdivisionsOf(court);
        return !found.isEmpty() && !found.contains(subdivisionId);
    }

    /** Return the subdivision of the apartment {@code code} by its start (BS before S). */
    public static String subdivisionOfApartment(String code) {
        if (code.startsWith("BS")) {
            return "bs";
        }
        if (code.startsWith("S")) {
            return "origami";
        }
        return null;
    }
}
