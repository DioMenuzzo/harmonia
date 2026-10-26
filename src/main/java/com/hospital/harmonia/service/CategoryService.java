package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.CategoryDao;
import com.hospital.harmonia.dao.impl.CategoryDaoImpl;
import com.hospital.harmonia.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Registry of the employee categories offered by the Colaboradores form.
 *
 * Names are stored uppercase and trimmed (normalize below), matching the
 * UPPER(TRIM(nome)) unique index in the database -- so "Alunos", "ALUNOS "
 * and "alunos" are the same category no matter which path created it.
 */
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryDao categoryDao;

    public CategoryService() {
        this(new CategoryDaoImpl());
    }

    /** Package-visible constructor allowing a test double to be injected (see CategoryServiceTest). */
    CategoryService(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
    }

    public Category register(Category category) {
        category.setName(normalize(category.getName()));
        validate(category);
        log.debug("Registering category: {}", category.getName());
        return categoryDao.save(category);
    }

    public void update(Category category) {
        category.setName(normalize(category.getName()));
        validate(category);
        categoryDao.update(category);
    }

    public void deactivate(int id) {
        categoryDao.deactivate(id);
    }

    public void activate(int id) {
        categoryDao.activate(id);
    }

    public List<Category> findAll() {
        return categoryDao.findAll();
    }

    /** Only the active ones -- what the employee form offers for NEW assignments. */
    public List<Category> findActive() {
        return categoryDao.findActive();
    }

    public Optional<Category> findById(int id) {
        return categoryDao.findById(id);
    }

    /** Case-insensitive -- used by the bulk import to resolve the category named in the file. */
    public Optional<Category> findByName(String name) {
        return name == null ? Optional.empty() : categoryDao.findByName(name);
    }

    public long countEmployees(int categoryId) {
        return categoryDao.countEmployees(categoryId);
    }

    /**
     * Uppercase + trimmed, the same shape the database's unique index compares
     * by. Applied on the way in (instead of only when reading) so every
     * category is stored in one canonical form and the report groups them as
     * a single block.
     */
    private static String normalize(String name) {
        return name == null ? null : name.trim().toUpperCase();
    }

    private void validate(Category category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new IllegalArgumentException("Nome da categoria é obrigatório.");
        }
        if (category.getName().length() > 150) {
            throw new IllegalArgumentException("Nome da categoria deve ter no máximo 150 caracteres.");
        }
    }
}
