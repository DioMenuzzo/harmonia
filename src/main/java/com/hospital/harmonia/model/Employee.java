package com.hospital.harmonia.model;

/** Contracted (outsourced) employee eligible for meals at the cafeteria. */
public class Employee {

    private Integer id;
    private String name;
    // NOTE: DB column is still "cpf" (legacy name) but this field actually
    // stores the employee's "matricula" (registration/badge number), not a
    // real CPF -- rename the DB column too when the database migration happens.
    private String registrationNumber;
    private String category;
    private String jobTitle; // no longer has a field on the form (removed by the user), kept for the DB column
    private boolean active = true;

    public Employee() {
    }

    public Employee(Integer id, String name, String registrationNumber, String category, String jobTitle) {
        this.id = id;
        this.name = name;
        this.registrationNumber = registrationNumber;
        this.category = category;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    @Override
    public String toString() {
        return name; // used in ComboBox/ListView
    }
}
