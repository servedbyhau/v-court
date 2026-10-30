package dev.vcourt.engine.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Ported from the desktop's {@code tests/test_catalog.py}. */
class CatalogTest {
    private static final Map<String, List<String>> ORIGAMI_COURTS = Map.of(
            "tennis", List.of("S6 - Sân tennis", "S8 - Sân tennis", "S9 - Sân tennis",
                    "S10 - Sân tennis"),
            "pickleball", List.of("S9 - Sân Pickleball"),
            "badminton", List.of("S6 - Sân cầu lông", "S8 - Sân Cầu lông", "S10 - Sân cầu lông"));

    private static List<Catalog.Court> courts() {
        List<Catalog.Court> all = new ArrayList<>();
        for (Catalog.Subdivision subdivision : Catalog.SUBDIVISIONS) {
            for (Catalog.Sport sport : subdivision.sports) {
                all.addAll(sport.courts());
            }
        }
        return all;
    }

    private static List<String> names(List<Catalog.Court> courts) {
        List<String> names = new ArrayList<>();
        for (Catalog.Court court : courts) {
            names.add(court.name);
        }
        return names;
    }

    @Test
    void everySportIsOneTheRunBooks() {
        for (Catalog.Subdivision subdivision : Catalog.SUBDIVISIONS) {
            Set<String> ids = new HashSet<>();
            for (Catalog.Sport sport : subdivision.sports) {
                assertTrue(Sports.isKnown(sport.id), subdivision.id + " " + sport.id);
                assertTrue(ids.add(sport.id), subdivision.id + " " + sport.id);
            }
        }
    }

    @Test
    void origamiKeepsEverySportAndItsCourts() {
        Catalog.Subdivision origami = Catalog.subdivision("origami");

        List<String> ids = new ArrayList<>();
        for (Catalog.Sport sport : origami.sports) {
            ids.add(sport.id);
        }
        assertEquals(new ArrayList<>(Sports.VIETNAMESE.keySet()), ids);
        for (Map.Entry<String, List<String>> entry : ORIGAMI_COURTS.entrySet()) {
            assertEquals(entry.getValue(), names(origami.sport(entry.getKey()).courts()));
        }
    }

    @Test
    void eachCourtIsListedOnceWithACodeAndAKnownKind() {
        List<String> names = names(courts());

        assertEquals(names.size(), new HashSet<>(names).size());
        for (Catalog.Court court : courts()) {
            assertFalse(court.code.isEmpty(), court.name);
            assertTrue(court.name.contains(court.code), court.name);
            assertTrue(court.kind.isEmpty() || Catalog.COURT_KINDS.contains(court.kind),
                    court.name);
        }
    }

    @Test
    void theCourtsOfASportAreToldApartByTheirLabel() {
        for (Catalog.Subdivision subdivision : Catalog.SUBDIVISIONS) {
            for (Catalog.Sport sport : subdivision.sports) {
                Set<String> labels = new HashSet<>();
                for (Catalog.Court court : sport.courts()) {
                    assertTrue(labels.add(court.code + "|" + court.kind + "|" + court.number),
                            subdivision.id + " " + sport.id);
                }
            }
        }
    }

    @Test
    void theKindsSplitFromTheNames() {
        List<Catalog.Court> basketball = Catalog.subdivision("origami").sport("basketball")
                .courts();

        List<String> labels = new ArrayList<>();
        for (Catalog.Court court : basketball.subList(0, 3)) {
            labels.add(court.code + "|" + court.kind + "|" + court.number);
        }
        assertEquals(List.of("S6|shooting|01", "S6|shooting|02", "S8|half|"), labels);
    }

    @Test
    void eachCourtNamesItsVenue() {
        assertEquals("BE _ Sân tennis", Catalog.venueOf("BE7 - Sân tennis"));
        assertEquals("BS _ Sân tennis", Catalog.venueOf("CX2 - Sân tennis 01"));
        assertEquals("Origami_ Sân tennis", Catalog.venueOf("S10 - Sân tennis"));
        assertNull(Catalog.venueOf("Khu B - Sân tennis"));
    }

    @Test
    void theDefaultCourtIsInTheCatalog() {
        assertNotNull(Catalog.venueOf("S10 - Sân tennis")); // the desktop default
        assertNull(Catalog.subdivision("khu-b"));
    }

    @Test
    void theApartmentCodeTellsTheSubdivision() {
        String[][] cases = {{"BS0101.01", "bs"}, {"S1.0101.01", "origami"}, {"BE7.0101", null}};
        for (String[] c : cases) {
            assertTrue(Catalog.APARTMENT.matcher(c[0]).matches(), c[0]);
            assertEquals(c[1], Catalog.subdivisionOfApartment(c[0]), c[0]);
        }
    }

    @Test
    void aCourtOutsideTheSubdivisionIsTold() {
        assertTrue(Catalog.courtOutside("bs", "S6 - Sân tennis"));
        assertFalse(Catalog.courtOutside("bs", "BE7 - Sân tennis"));
        assertFalse(Catalog.courtOutside("bs", "Khu B - Sân tennis")); // not in the catalog
    }
}
