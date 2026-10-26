package com.hospital.harmonia.model;

import java.util.Objects;

/**
 * Employee category (the group an employee is billed under: "ALUNOS",
 * "UNICAMP", "FUNCIONARIOS"...).
 *
 * Categories used to be free text typed into the employee form, which meant
 * the same group could be recorded as "UNICAMP", "UNICAMP LTDA" and "UNICAM"
 * -- each one becoming a separate block with its own subtotal on the
 * cafeteria report used to bill the outsourced companies. They are now a
 * registry of their own (table "categorias"), picked from a dropdown, so a
 * typo can't invent a new category.
 *
 * Deactivated instead of deleted (same pattern as Employee/Meal): a category
 * no longer in use must still resolve for the employees and reports that
 * already reference it.
 */
public class Category {

    private Integer id;
    private String name;
    private boolean active = true;

    public Category() {
    }

    public Category(Integer id, String name) {
        this.id = id;
        this.name = name;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * By id, so a Category read in one query equals the same category read in
     * another -- the employee form needs this to preselect the right item in
     * the dropdown (the instance loaded with the employee is never the same
     * object as the one in the ComboBox's item list).
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Category)) {
            return false;
        }
        Category that = (Category) other;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return name; // used in ComboBox/ListView
    }
}
