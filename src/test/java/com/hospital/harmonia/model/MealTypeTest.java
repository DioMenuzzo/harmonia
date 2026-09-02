package com.hospital.harmonia.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the cafeteria's operating-hours logic (MealType.byTime), including
 * the exact boundaries of each window (inclusive start, exclusive end) and
 * the gaps in between where the cafeteria is closed.
 */
class MealTypeTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "00:00, CEIA",
            "03:00, CEIA",
            "05:59, CEIA",
            "06:00, CAFE_DA_MANHA",
            "09:59, CAFE_DA_MANHA",
            "11:30, ALMOCO",
            "14:29, ALMOCO",
            "18:30, JANTAR",
            "21:29, JANTAR",
    })
    void byTime_returnsTheExpectedTypeInsideEachWindow(String time, MealType expected) {
        assertEquals(expected, MealType.byTime(LocalTime.parse(time)));
    }

    @ParameterizedTest(name = "{0} is outside operating hours")
    @CsvSource({"10:00", "11:29", "14:30", "18:29", "21:30", "23:59"})
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
