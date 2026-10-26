package com.hospital.harmonia.dao;

import com.hospital.harmonia.model.Contact;

import java.util.List;
import java.util.Optional;

public interface ContactDao {
    Contact save(Contact contact);
    void update(Contact contact);
    List<Contact> findAll();
    Optional<Contact> findById(int id);
    /** Kept for occasional administrative use (not called by the screen) -- the normal flow is deactivate/activate. */
    void remove(int id);
    void deactivate(int id);
    void activate(int id);
}
