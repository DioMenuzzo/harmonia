package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.ContactDao;
import com.hospital.harmonia.dao.impl.ContactDaoImpl;
import com.hospital.harmonia.model.Contact;

import java.util.List;
import java.util.Optional;

public class ContactService {

    private final ContactDao contactDao;

    public ContactService() {
        this(new ContactDaoImpl());
    }

    /** Package-visible constructor allowing a test double to be injected (see ContactServiceTest). */
    ContactService(ContactDao contactDao) {
        this.contactDao = contactDao;
    }

    public Contact register(Contact contact) {
        validate(contact);
        return contactDao.save(contact);
    }

    public void update(Contact contact) {
        validate(contact);
        contactDao.update(contact);
    }

    public List<Contact> findAll() {
        return contactDao.findAll();
    }

    public Optional<Contact> findById(int id) {
        return contactDao.findById(id);
    }

    /** Kept for occasional administrative use -- the normal flow is deactivate/activate. */
    public void remove(int id) {
        contactDao.remove(id);
    }

    public void deactivate(int id) {
        contactDao.deactivate(id);
    }

    public void activate(int id) {
        contactDao.activate(id);
    }

    private void validate(Contact contact) {
        if (contact.getName() == null || contact.getName().isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório.");
        }
        if (contact.getEmail() != null && !contact.getEmail().isBlank() && !contact.getEmail().contains("@")) {
            throw new IllegalArgumentException("E-mail inválido.");
        }
    }
}
