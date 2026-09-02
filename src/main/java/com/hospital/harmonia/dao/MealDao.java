package com.hospital.harmonia.dao;

import com.hospital.harmonia.model.Meal;

import java.time.LocalDate;
import java.util.List;

public interface MealDao {
    Meal save(Meal meal);
    void update(Meal meal);
    void remove(int id);
    void deactivate(int id);
    void activate(int id);
    /** All meals in the period, active and inactive -- used in the screen's table/audit log. */
    List<Meal> findByPeriod(LocalDate start, LocalDate end);
    /** Only the active meals in the period -- used in the PDF report and the total spent. */
    List<Meal> findActiveByPeriod(LocalDate start, LocalDate end);
    List<Meal> findByEmployeeAndPeriod(int employeeId, LocalDate start, LocalDate end);
}
