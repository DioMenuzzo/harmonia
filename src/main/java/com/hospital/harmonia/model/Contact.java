package com.hospital.harmonia.model;

/**
 * Contact in the Phone Directory (Agenda Telefônica) module -- hospital
 * sectors, doctors, partner institutions, ambulance/transport services and
 * any other useful phone number, replacing the standalone Excel spreadsheet
 * ("Agenda v2.0") the front desk used to keep this in before this module
 * existed.
 *
 * The category is its own registry now (table "categorias_agenda", picked
 * from a dropdown on the module's own Categorias tab) instead of free text --
 * same pattern as Employee.categoryId/categoryName. categoryName is the
 * denormalized name that comes with it from the JOIN: the table column and
 * the filters read the name without needing a second query, while
 * categoryId is what actually gets written.
 */
public class Contact {

    private Integer id;
    private String name;
    private Integer categoryId;
    private String categoryName;
    private String cellPhone;
    private String phone;
    private String email;
    private String note;
    // Soft delete instead of permanent deletion, to keep the history -- same
    // pattern as Employee/Meal/Equipment.
    private boolean active = true;

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

    public String getCellPhone() {
        return cellPhone;
    }

    public void setCellPhone(String cellPhone) {
        this.cellPhone = cellPhone;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
