package com.hospital.harmonia.service;

/**
 * Thrown by MealService when a meal would violate the one-meal-per-type-
 * per-employee-per-day rule (see MealService.checkNoDuplicateMealType).
 *
 * A dedicated subtype of IllegalArgumentException -- instead of throwing a
 * plain IllegalArgumentException like Meal's other validation rules do --
 * so that a caller who needs to react to *this specific* failure
 * differently can catch it by type. CafeteriaController.onImportMeals()
 * does exactly that: it pulls these conflicts out into their own dedicated
 * summary dialog (mirroring the missing-registration one) instead of
 * mixing them into the generic, capped, line-by-line import problem list.
 * Any existing code that catches IllegalArgumentException (or just
 * Exception) still catches this unchanged, since it IS an
 * IllegalArgumentException.
 */
public class DuplicateMealException extends IllegalArgumentException {

    public DuplicateMealException(String message) {
        super(message);
    }
}
