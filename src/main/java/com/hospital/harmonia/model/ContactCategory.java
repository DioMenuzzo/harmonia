package com.hospital.harmonia.model;

import java.util.Objects;

/**
 * Category of a Phone Directory (Agenda Telefônica) contact -- the type/origin
 * tag shown and managed on that module's own "Categorias" tab (AMBULÂNCIAS,
 * HOSPITAL, LABORATÓRIO...), mirroring how Category/"categorias" works for
 * Refeitório's employees.
 *
 * Deliberately a SEPARATE registry from Category/"categorias" (employee
 * billing groups): the two represent different concepts (an employee's
 * billing group vs. a contact's type/origin) that happen to share the same
 * shape (id/name/active), so sharing one table would conflate two unrelated
 * lists -- inactivating a billing category could never accidentally affect
 * the Agenda's categories, and vice versa, this way.
 *
 * Deactivated instead of deleted (same pattern as Category/Employee/Meal): a
 * category no longer in use must still resolve for the contacts that already
 * reference it.
 */
public class ContactCategory {

    private Integer id;
    private String name;
    private boolean active = true;

    public ContactCategory() {
    }

    public ContactCategory(Integer id, String name) {
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
     * By id, so a ContactCategory read in one query equals the same category
     * read in another -- the contact form needs this to preselect the right
     * item in the dropdown (the instance loaded with the contact is never the
     * same object as the one in the ComboBox's item list).
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ContactCategory)) {
            return false;
        }
        ContactCategory that = (ContactCategory) other;
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
