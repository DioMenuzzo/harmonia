package com.hospital.harmonia.model;

import java.math.BigDecimal;
import java.time.LocalTime;

public enum MealType {

    // PLACEHOLDER: provisional prices, adjust once the official values are defined.
    // NOTE: constant names stay as-is -- they must match the literal values
    // already stored in the "tipo" column (and its CHECK constraint) in the
    // database. Rename these together with the database migration when that happens.
    CEIA("Ceia", new BigDecimal("10.00")),
    CAFE_DA_MANHA("Cafe da manha", new BigDecimal("8.00")),
    ALMOCO("Almoco", new BigDecimal("18.00")),
    JANTAR("Jantar", new BigDecimal("15.00")),
    // No longer assigned automatically (see byTime) -- kept in the enum for
    // compatibility with old records and with the database constraint.
    LANCHE("Lanche", new BigDecimal("6.00"));

    private final String description;
    private final BigDecimal defaultPrice;

    MealType(String description, BigDecimal defaultPrice) {
        this.description = description;
        this.defaultPrice = defaultPrice;
    }

    public String getDescription() {
        return description;
    }

    /** Default price for the meal type. PLACEHOLDER -- adjust once the official values are defined. */
    public BigDecimal getDefaultPrice() {
        return defaultPrice;
    }

    @Override
    public String toString() {
        return description;
    }

    /**
     * Determines the meal type from the given time, according to how the
     * cafeteria operates (intervals with inclusive start and exclusive end):
     * <pre>
     *   00:00 - 06:00  -> Ceia (late supper)
     *   06:00 - 10:00  -> Cafe da manha (breakfast)
     *   11:30 - 14:30  -> Almoco (lunch)
     *   18:30 - 21:30  -> Jantar (dinner)
     * </pre>
     * Outside these windows the cafeteria is closed; in that case an
     * {@link IllegalArgumentException} is thrown instead of assigning a type.
     *
     * @throws IllegalArgumentException if the time is null or falls outside the cafeteria's operating hours
     */
    public static MealType byTime(LocalTime time) {
        if (time == null) {
            throw new IllegalArgumentException("Informe o horario da refeicao.");
        }
        if (within(time, LocalTime.of(0, 0), LocalTime.of(6, 0))) {
            return CEIA;
        }
        if (within(time, LocalTime.of(6, 0), LocalTime.of(10, 0))) {
            return CAFE_DA_MANHA;
        }
        if (within(time, LocalTime.of(11, 30), LocalTime.of(14, 30))) {
            return ALMOCO;
        }
        if (within(time, LocalTime.of(18, 30), LocalTime.of(21, 30))) {
            return JANTAR;
        }
        throw new IllegalArgumentException(
                "Refeitorio fechado nesse horario (" + time + "). Horarios de funcionamento: "
                        + "00:00-06:00 (Ceia), 06:00-10:00 (Cafe da manha), "
                        + "11:30-14:30 (Almoco) e 18:30-21:30 (Jantar).");
    }

    /** [startInclusive, endExclusive) */
    private static boolean within(LocalTime time, LocalTime startInclusive, LocalTime endExclusive) {
        return !time.isBefore(startInclusive) && time.isBefore(endExclusive);
    }
}
