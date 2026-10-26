package com.hospital.harmonia.model;

import java.math.BigDecimal;
import java.time.LocalTime;

public enum MealType {

    // PLACEHOLDER: provisional prices, adjust once the official values are defined.
    // NOTE: constant names stay as-is -- they must match the literal values
    // already stored in the "tipo" column (and its CHECK constraint) in the
    // database. Rename these together with the database migration when that happens.
    CEIA("Ceia", new BigDecimal("10.00")),
    CAFE_DA_MANHA("Dejejum", new BigDecimal("8.00")),
    ALMOCO("Almoço", new BigDecimal("18.00")),
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

    // Tolerance agreed with HR: an employee can clock in up to this many
    // minutes before the start or after the end of each meal's official
    // time window and it still counts as within that window (instead of
    // rejecting the record over a difference of a few minutes). Applied to
    // both ends of every window -- see within().
    private static final int TOLERANCE_MINUTES = 20;

    /**
     * Determines the meal type from the given time, according to how the
     * cafeteria operates (official intervals, inclusive start and exclusive
     * end -- see TOLERANCE_MINUTES above for the extra grace period applied
     * to both ends of each window below):
     * <pre>
     *   23:00 - 03:00  -> Ceia (late supper) -- wraps past midnight
     *   04:00 - 06:00  -> Dejejum (breakfast)
     *   11:00 - 15:30  -> Almoço (lunch)
     *   18:00 - 20:00  -> Jantar (dinner)
     * </pre>
     * Outside these windows (plus tolerance) the cafeteria is closed; in that
     * case an {@link IllegalArgumentException} is thrown instead of
     * assigning a type.
     *
     * @throws IllegalArgumentException if the time is null or falls outside the cafeteria's operating hours (with tolerance)
     */
    public static MealType byTime(LocalTime time) {
        if (time == null) {
            throw new IllegalArgumentException("Informe o horário da refeição.");
        }
        if (within(time, LocalTime.of(23, 0), LocalTime.of(3, 0))) {
            return CEIA;
        }
        if (within(time, LocalTime.of(4, 0), LocalTime.of(6, 0))) {
            return CAFE_DA_MANHA;
        }
        if (within(time, LocalTime.of(11, 0), LocalTime.of(15, 30))) {
            return ALMOCO;
        }
        if (within(time, LocalTime.of(18, 0), LocalTime.of(20, 0))) {
            return JANTAR;
        }
        throw new IllegalArgumentException(
                "Refeitório fechado nesse horário (" + time + "). Horários de funcionamento: "
                        + "23:00-03:00 (Ceia), 04:00-06:00 (Dejejum), "
                        + "11:00-15:30 (Almoço) e 18:00-20:00 (Jantar) "
                        + "(considerando " + TOLERANCE_MINUTES + " minutos de tolerância antes/depois de cada horário).");
    }

    /**
     * [startInclusive, endExclusive), with TOLERANCE_MINUTES of slack applied
     * to both ends (startInclusive gets earlier, endExclusive gets later).
     * Handles a window that wraps past midnight (start later than end in the
     * clock, e.g. 23:00 -> 03:00 for Ceia): in that case the time matches if
     * it's at/after the start OR before the end, instead of requiring both
     * like a same-day window.
     */
    private static boolean within(LocalTime time, LocalTime startInclusive, LocalTime endExclusive) {
        LocalTime toleratedStart = startInclusive.minusMinutes(TOLERANCE_MINUTES);
        LocalTime toleratedEnd = endExclusive.plusMinutes(TOLERANCE_MINUTES);
        if (!toleratedStart.isAfter(toleratedEnd)) {
            return !time.isBefore(toleratedStart) && time.isBefore(toleratedEnd);
        }
        return !time.isBefore(toleratedStart) || time.isBefore(toleratedEnd);
    }
}
