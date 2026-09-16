package com.hospital.harmonia.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Individual record of a meal consumed by a contracted employee. */
public class Meal {

    private Integer id;
    private Integer employeeId;
    private String employeeName; // denormalized to make it easier to display in tables/reports
    private String employeeCategory; // denormalized -- same reasoning as employeeName (filters/report column)
    private LocalDate date;
    private LocalTime time;
    private BigDecimal price;
    private MealType type;
    private boolean active = true;
    // Denormalized from the employee's own Employee.sharedUsage (same
    // reasoning as employeeName/employeeCategory above), captured at the
    // moment this meal is registered/updated so MealService can decide
    // whether the one-meal-per-type-per-day rule applies without needing
    // its own EmployeeDao dependency -- see MealService.checkNoDuplicateMealType.
    private boolean employeeSharedUsage = false;

    public Meal() {
    }

    public Meal(Integer id, Integer employeeId, String employeeName, LocalDate date,
                LocalTime time, BigDecimal price, MealType type) {
        this.id = id;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.date = date;
        this.time = time;
        this.price = price;
        this.type = type;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeCategory() {
        return employeeCategory;
    }

    public void setEmployeeCategory(String employeeCategory) {
        this.employeeCategory = employeeCategory;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public MealType getType() {
        return type;
    }

    public void setType(MealType type) {
        this.type = type;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isEmployeeSharedUsage() {
        return employeeSharedUsage;
    }

    public void setEmployeeSharedUsage(boolean employeeSharedUsage) {
        this.employeeSharedUsage = employeeSharedUsage;
    }
}
