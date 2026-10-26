package com.hospital.harmonia.dao;

import com.hospital.harmonia.model.ContactCategory;

import java.util.List;
import java.util.Optional;

public interface ContactCategoryDao {

    ContactCategory save(ContactCategory category);

    void update(ContactCategory category);

    Optional<ContactCategory> findById(int id);

    /** Case-insensitive lookup -- same convenience CategoryDao offers, kept for parity even if unused today. */
    Optional<ContactCategory> findByName(String name);

    List<ContactCategory> findAll();

    List<ContactCategory> findActive();

    void deactivate(int id);

    void activate(int id);

    /** How many contacts currently point at this category (shown on the Categorias tab). */
    long countContacts(int categoryId);
}
