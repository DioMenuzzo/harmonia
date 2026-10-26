package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.CategoryDao;
import com.hospital.harmonia.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises CategoryService's normalization and validation with an in-memory
 * fake instead of a real database (see FakeCategoryDao below).
 *
 * The normalization is the point of the whole category registry: the report
 * that bills the outsourced companies groups by category, so "Alunos" and
 * "ALUNOS " reaching the database as two separate rows is what used to split
 * one company into two blocks with two subtotals.
 */
class CategoryServiceTest {

    private FakeCategoryDao fakeDao;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        fakeDao = new FakeCategoryDao();
        service = new CategoryService(fakeDao);
    }

    private static Category named(String name) {
        Category category = new Category();
        category.setName(name);
        return category;
    }

    @Test
    void register_storesTheNameUppercased() {
        service.register(named("Alunos"));
        assertEquals("ALUNOS", fakeDao.saved.get(0).getName());
    }

    @Test
    void register_trimsSurroundingSpaces() {
        service.register(named("   Unicamp   "));
        assertEquals("UNICAMP", fakeDao.saved.get(0).getName());
    }

    @Test
    void register_normalizesSoTwoSpellingsBecomeTheSameName() {
        service.register(named("Alunos"));
        service.register(named("  ALUNOS "));
        // Both reach the DAO under one name -- the unique index on
        // UPPER(TRIM(nome)) is what actually rejects the second one, and this
        // is what lets it recognize them as the same category.
        assertEquals(fakeDao.saved.get(0).getName(), fakeDao.saved.get(1).getName());
    }

    @Test
    void register_rejectsAnEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> service.register(named("   ")));
        assertTrue(fakeDao.saved.isEmpty(), "nothing should reach the DAO");
    }

    @Test
    void register_rejectsANameWithoutText() {
        assertThrows(IllegalArgumentException.class, () -> service.register(named(null)));
    }

    @Test
    void register_rejectsANameLongerThanTheColumn() {
        assertThrows(IllegalArgumentException.class, () -> service.register(named("A".repeat(151))));
    }

    @Test
    void update_normalizesTheNameToo() {
        service.update(named("  funcionarios "));
        assertEquals("FUNCIONARIOS", fakeDao.updated.get(0).getName());
    }

    @Test
    void findByName_returnsEmptyForANullName() {
        assertTrue(service.findByName(null).isEmpty());
    }

    /** Minimal in-memory stand-in for CategoryDaoImpl -- no database involved. */
    private static class FakeCategoryDao implements CategoryDao {
        final List<Category> saved = new ArrayList<>();
        final List<Category> updated = new ArrayList<>();
        private int nextId = 1;

        @Override
        public Category save(Category category) {
            category.setId(nextId++);
            saved.add(category);
            return category;
        }

        @Override
        public void update(Category category) {
            updated.add(category);
        }

        @Override
        public Optional<Category> findById(int id) {
            return saved.stream().filter(c -> Integer.valueOf(id).equals(c.getId())).findFirst();
        }

        @Override
        public Optional<Category> findByName(String name) {
            return saved.stream().filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(name.trim()))
                    .findFirst();
        }

        @Override
        public List<Category> findAll() {
            return List.copyOf(saved);
        }

        @Override
        public List<Category> findActive() {
            return saved.stream().filter(Category::isActive).toList();
        }

        @Override
        public void deactivate(int id) {
            findById(id).ifPresent(c -> c.setActive(false));
        }

        @Override
        public void activate(int id) {
            findById(id).ifPresent(c -> c.setActive(true));
        }

        @Override
        public long countEmployees(int categoryId) {
            return 0L;
        }
    }
}
