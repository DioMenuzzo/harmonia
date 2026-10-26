package com.hospital.harmonia.service;

import com.hospital.harmonia.model.Employee;
import com.hospital.harmonia.model.Meal;
import com.hospital.harmonia.model.MealType;

import java.util.Set;

/**
 * Optional filters applied on top of the mandatory date range when
 * generating a cafeteria report (PDF or XLSX) -- see
 * MealService.generatePeriodReport / generatePeriodReportXlsx.
 *
 * Category and type each support multiple values (e.g. "Alunos" + "Unicamp")
 * and can work either as an inclusion list (only meals matching one of the
 * selected values) or as an exclusion list (every meal EXCEPT the selected
 * values) -- see excludeCategories/excludeTypes. An empty set for either
 * means "no filter on that field", regardless of its exclude flag (so
 * turning "exclude" on with nothing selected still means "everyone").
 * Employee stays a single optional value, same as before this feature.
 */
public record ReportFilters(Set<String> categories, boolean excludeCategories,
                             Set<MealType> types, boolean excludeTypes,
                             Employee employee) {

    /** No filters at all -- every active meal in the period matches. */
    public static ReportFilters none() {
        return new ReportFilters(Set.of(), false, Set.of(), false, null);
    }

    /** Whether the given meal satisfies every filter set here. */
    public boolean matches(Meal meal) {
        if (!categories.isEmpty()) {
            boolean selected = categories.stream().anyMatch(c -> c.equalsIgnoreCase(meal.getEmployeeCategory()));
            if (selected == excludeCategories) {
                return false;
            }
        }
        if (!types.isEmpty()) {
            boolean selected = types.contains(meal.getType());
            if (selected == excludeTypes) {
                return false;
            }
        }
        return employee == null || employee.getId().equals(meal.getEmployeeId());
    }
}
