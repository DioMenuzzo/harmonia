package com.hospital.harmonia.dao;

import com.hospital.harmonia.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryDao {

    Category save(Category category);

    void update(Category category);

    Optional<Category> findById(int id);

    /** Case-insensitive lookup -- used by the bulk import to resolve the category named in the file. */
    Optional<Category> findByName(String name);

    List<Category> findAll();

    List<Category> findActive();

    void deactivate(int id);

    void activate(int id);

    /** How many employees currently point at this category (shown on the Categorias tab). */
    long countEmployees(int categoryId);
}
