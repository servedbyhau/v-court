package dev.vcourt.engine.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import dev.vcourt.engine.screen.Geometry;

/** Ported from the desktop's {@code tests/test_app_language.py}. */
class AppLanguageTest {
    private static final List<AppLanguage> LANGUAGES =
            AppLanguage.of(Labels.vietnamese(), "tennis");
    private static final AppLanguage VIETNAMESE = LANGUAGES.get(0);
    private static final AppLanguage ENGLISH = LANGUAGES.get(1);

    private static boolean matches(String regex, String text) {
        return Pattern.compile(regex, Pattern.DOTALL).matcher(text).matches();
    }

    @Test
    void everyEnglishLabelIsSet() {
        Labels english = AppLanguage.english();
        for (String key : Labels.KEYS) {
            assertFalse(english.get(key).isEmpty(), key);
            assertNotEquals(Labels.vietnamese().get(key), english.get(key), key);
        }
    }

    @Test
    void everySportHasAnEnglishTile() {
        assertEquals(Sports.VIETNAMESE.keySet(), Sports.ENGLISH.keySet());
        assertTrue(Sports.ENGLISH.values().stream().noneMatch(String::isEmpty));
    }

    @Test
    void englishDatesMatchTheExactDayOnly() {
        String pattern = ENGLISH.dayPattern(LocalDate.of(2026, 10, 3));
        assertTrue(matches(pattern, "3, Saturday, October 3, 2026"));
        assertFalse(matches(pattern, "13, Tuesday, October 13, 2026"));
        assertFalse(matches(pattern, "3, Tuesday, November 3, 2026"));
        assertFalse(matches(pattern, "3, Thứ Bảy, 3 tháng 10, 2026"));
    }

    @Test
    void englishDatesAcrossMonthAndYear() {
        assertTrue(matches(ENGLISH.dayPattern(LocalDate.of(2026, 10, 21)),
                "21, Wednesday, October 21, 2026"));
        assertTrue(matches(ENGLISH.dayPattern(LocalDate.of(2026, 11, 1)),
                "1, Sunday, November 1, 2026"));
        assertTrue(matches(ENGLISH.dayPattern(LocalDate.of(2027, 1, 1)),
                "1, Friday, January 1, 2027"));
    }

    @Test
    void anyDayMatchesTheDaysOfItsLanguage() {
        String[][] cases = {
            {"2, Thứ Sáu, 2 tháng 10, 2026", "true", "false"},
            {"21, Wednesday, October 21, 2026", "false", "true"},
            {"18:00 - 19:00\nFull", "false", "false"},
            {"Select year", "false", "false"},
        };
        for (String[] c : cases) {
            assertEquals(Boolean.parseBoolean(c[1]), matches(VIETNAMESE.anyDay(), c[0]), c[0]);
            assertEquals(Boolean.parseBoolean(c[2]), matches(ENGLISH.anyDay(), c[0]), c[0]);
        }
    }

    @Test
    void englishMonthIndexFromLabel() {
        assertEquals(Geometry.monthIndex(2026, 10),
                ENGLISH.monthIndexFromLabel("21, Wednesday, October 21, 2026"));
        assertEquals(Geometry.monthIndex(2027, 1),
                ENGLISH.monthIndexFromLabel("1, Friday, January 1, 2027"));
        assertNull(ENGLISH.monthIndexFromLabel("2, Thứ Sáu, 2 tháng 10, 2026"));
    }

    @Test
    void vietnameseUsesTheSettingsLabels() {
        Labels labels = Labels.of(Labels.vietnamese(),
                Map.of("home", "Trang chủ", "utility", "Sân tennis"));

        assertEquals(labels, AppLanguage.of(labels, "tennis").get(0).labels());
    }

    @Test
    void eachLanguageTapsTheTileOfTheSport() {
        String[][] cases = {
            {"tennis", "Sân Tennis", "Tennis"},
            {"table-tennis", "Sân bóng bàn", "Pingpong"},
            {"pickleball", "Sân Pickleball", "Pickleball"},
            {"badminton", "Sân cầu lông", "Badminton"},
        };
        for (String[] c : cases) {
            List<AppLanguage> languages = AppLanguage.of(Labels.vietnamese(), c[0]);
            assertEquals(c[1], languages.get(0).labels().utility());
            assertEquals(c[2], languages.get(1).labels().utility());
        }
    }

    @Test
    void aUtilityLabelSetByHandWinsOverTheSport() {
        Labels labels = Labels.vietnamese().with("utility", "Sân mới");

        assertEquals("Sân mới", AppLanguage.of(labels, "badminton").get(0).labels().utility());
    }

    @Test
    void anUnknownSportIsRefused() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AppLanguage.of(Labels.vietnamese(), "golf"));
        assertTrue(e.getMessage().startsWith("Unknown sport 'golf'"));
    }

    @Test
    void anUnknownLabelIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> Labels.vietnamese().with("zoom", "x"));
    }

    @Test
    void dayPatternMatchesExactDayOnly() {
        String pattern = VIETNAMESE.dayPattern(LocalDate.of(2026, 10, 3));
        assertTrue(matches(pattern, "3, Thứ Bảy, 3 tháng 10, 2026"));
        assertFalse(matches(pattern, "13, Thứ Ba, 13 tháng 10, 2026"));
        assertFalse(matches(pattern, "3, Thứ Ba, 3 tháng 11, 2026"));
    }

    @Test
    void dayPatternAcrossMonthAndYear() {
        assertTrue(matches(VIETNAMESE.dayPattern(LocalDate.of(2026, 11, 1)),
                "1, Chủ Nhật, 1 tháng 11, 2026"));
        assertTrue(matches(VIETNAMESE.dayPattern(LocalDate.of(2027, 1, 1)),
                "1, Thứ Sáu, 1 tháng 1, 2027"));
    }

    @Test
    void anyDayMatchesDaysOnly() {
        assertTrue(matches(VIETNAMESE.anyDay(), "2, Thứ Sáu, 2 tháng 10, 2026"));
        assertTrue(matches(VIETNAMESE.anyDay(), "31, Thứ Bảy, 31 tháng 12, 2026"));
        assertFalse(matches(VIETNAMESE.anyDay(), "18:00 - 19:00"));
        assertFalse(matches(VIETNAMESE.anyDay(), "Chọn năm"));
    }

    @Test
    void monthIndexFromLabel() {
        assertEquals(Geometry.monthIndex(2026, 10),
                VIETNAMESE.monthIndexFromLabel("2, Thứ Sáu, 2 tháng 10, 2026"));
        assertEquals(Geometry.monthIndex(2027, 1),
                VIETNAMESE.monthIndexFromLabel("1, Thứ Sáu, 1 tháng 1, 2027"));
        assertNull(VIETNAMESE.monthIndexFromLabel("no date"));
    }
}
