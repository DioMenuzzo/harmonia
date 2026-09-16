package com.hospital.harmonia.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the cafeteria's operating-hours logic (MealType.byTime), including
 * the exact boundaries of each window (inclusive start, exclusive end), the
 * 20-minute tolerance agreed with RH on both sides of every window (see
 * MealType.TOLERANCE_MINUTES), and the gaps in between where the cafeteria
 * is still closed even with that tolerance.
 *
 * Official windows (see MealType.byTime's Javadoc for the source of truth):
 *   23:00 - 03:00  -> Ceia (wraps past midnight)
 *   04:00 - 06:00  -> Dejejum
 *   11:00 - 15:30  -> Almoço
 *   18:00 - 20:00  -> Jantar
 */
class MealTypeTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "23:00, CEIA",
            "00:00, CEIA",
            "02:59, CEIA",
            "04:00, CAFE_DA_MANHA",
            "05:00, CAFE_DA_MANHA",
            "05:59, CAFE_DA_MANHA",
            "11:00, ALMOCO",
            "13:00, ALMOCO",
            "15:29, ALMOCO",
            "18:00, JANTAR",
            "19:00, JANTAR",
            "19:59, JANTAR",
    })
    void byTime_returnsTheExpectedTypeInsideEachOfficialWindow(String time, MealType expected) {
        assertEquals(expected, MealType.byTime(LocalTime.parse(time)));
    }

    // Times outside the official window but still within the 20-minute
    // tolerance on either side -- must still resolve to the corresponding
    // meal type instead of throwing.
    @ParameterizedTest(name = "{0} -> {1} (dentro da tolerância)")
    @CsvSource({
            "22:40, CEIA",
            "03:19, CEIA",
            "03:40, CAFE_DA_MANHA",
            "06:19, CAFE_DA_MANHA",
            "10:40, ALMOCO",
            "15:49, ALMOCO",
            "17:40, JANTAR",
            "20:19, JANTAR",
    })
    void byTime_returnsTheExpectedTypeWithinTheToleranceWindow(String time, MealType expected) {
        assertEquals(expected, MealType.byTime(LocalTime.parse(time)));
    }

    @ParameterizedTest(name = "{0} is outside operating hours (mesmo com tolerância)")
    @CsvSource({"22:39", "03:20", "03:30", "03:39", "06:20", "10:00", "10:39", "15:50", "17:00", "17:39", "20:20", "22:00"})
    void byTime_throwsWhenTheCafeteriaIsClosed(String time) {
        assertThrows(IllegalArgumentException.class, () -> MealType.byTime(LocalTime.parse(time)));
    }

    @Test
    void byTime_throwsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> MealType.byTime(null));
    }

    @Test
    void everyConstantHasADescriptionAndAPositiveDefaultPrice() {
        for (MealType type : MealType.values()) {
            assertEquals(type.getDescription(), type.toString());
            assertEquals(1, type.getDefaultPrice().signum(), () -> type + " default price should be positive");
        }
    }
}
