package com.hospital.harmonia.model;

/** Contracted (outsourced) employee eligible for meals at the cafeteria. */
public class Employee {

    private Integer id;
    private String name;
    // NOTE: DB column is still "cpf" (legacy name) but this field actually
    // stores the employee's "matricula" (registration/badge number), not a
    // real CPF -- rename the DB column too when the database migration happens.
    private String registrationNumber;
    // The category is a registry of its own now (table "categorias", picked
    // from a dropdown) instead of free text, so what the employee record holds
    // is the link. categoryName is the denormalized name that comes with it
    // from the JOIN -- same pattern as Meal.employeeName/employeeCategory:
    // the table column, the filters and the report read the name without
    // needing a second query, while categoryId is what actually gets written.
    private Integer categoryId;
    private String categoryName;
    private String jobTitle; // no longer has a field on the form (removed by the user), kept for the DB column
    private boolean active = true;
    // Some registrations (matrícula/crachá) are intentionally shared by more
    // than one person -- e.g. a "plantão médico" badge used by whichever
    // doctor is on call, not tied to a single individual. When true, the
    // one-meal-per-type-per-day rule (MealService.checkNoDuplicateMealType)
    // is skipped for meals registered under this employee, since several
    // different people legitimately eating under the same matrícula on the
    // same day is expected here, not a duplicate.
    private boolean sharedUsage = false;

    public Employee() {
    }

    public Employee(Integer id, String name, String registrationNumber, String categoryName, String jobTitle) {
        this.id = id;
        this.name = name;
        this.registrationNumber = registrationNumber;
        this.categoryName = categoryName;
        this.jobTitle = jobTitle;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    /** Denormalized from the JOIN -- read-only as far as persistence goes; setCategoryId is what gets saved. */
    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isSharedUsage() {
        return sharedUsage;
    }

    public void setSharedUsage(boolean sharedUsage) {
        this.sharedUsage = sharedUsage;
    }

    @Override
    public String toString() {
        return name; // used in ComboBox/ListView
    }
}
