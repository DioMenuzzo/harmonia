package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.ContactCategoryDao;
import com.hospital.harmonia.dao.impl.ContactCategoryDaoImpl;
import com.hospital.harmonia.model.ContactCategory;

import java.util.List;
import java.util.Optional;

/**
 * Registry of the categories offered by the Agenda Telefônica form's
 * Categoria dropdown. Mirrors CategoryService -- see ContactCategory's
 * Javadoc for why this is a separate registry from employee categories.
 *
 * Names are stored uppercase and trimmed (normalize below), matching the
 * UPPER(TRIM(nome)) unique index in the database -- so "Hospital",
 * "HOSPITAL " and "hospital" are the same category no matter which path
 * created it.
 */
public class ContactCategoryService {

    private final ContactCategoryDao contactCategoryDao;

    public ContactCategoryService() {
        this(new ContactCategoryDaoImpl());
    }

    /** Package-visible constructor allowing a test double to be injected. */
    ContactCategoryService(ContactCategoryDao contactCategoryDao) {
        this.contactCategoryDao = contactCategoryDao;
    }

    public ContactCategory register(ContactCategory category) {
        category.setName(normalize(category.getName()));
        validate(category);
        return contactCategoryDao.save(category);
    }

    public void update(ContactCategory category) {
        category.setName(normalize(category.getName()));
        validate(category);
        contactCategoryDao.update(category);
    }

    public void deactivate(int id) {
        contactCategoryDao.deactivate(id);
    }

    public void activate(int id) {
        contactCategoryDao.activate(id);
    }

    public List<ContactCategory> findAll() {
        return contactCategoryDao.findAll();
    }

    /** Only the active ones -- what the contact form offers for NEW assignments. */
    public List<ContactCategory> findActive() {
        return contactCategoryDao.findActive();
    }

    public Optional<ContactCategory> findById(int id) {
        return contactCategoryDao.findById(id);
    }

    public Optional<ContactCategory> findByName(String name) {
        return name == null ? Optional.empty() : contactCategoryDao.findByName(name);
    }

    public long countContacts(int categoryId) {
        return contactCategoryDao.countContacts(categoryId);
    }

    private static String normalize(String name) {
        return name == null ? null : name.trim().toUpperCase();
    }

    private void validate(ContactCategory category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new IllegalArgumentException("Nome da categoria é obrigatório.");
        }
        if (category.getName().length() > 150) {
            throw new IllegalArgumentException("Nome da categoria deve ter no máximo 150 caracteres.");
        }
    }
}
