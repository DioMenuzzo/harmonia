package com.hospital.harmonia.dao;

import com.hospital.harmonia.model.Equipment;

import java.util.List;
import java.util.Optional;

public interface EquipmentDao {
    Equipment save(Equipment equipment);
    void update(Equipment equipment);
    List<Equipment> findAll();
    Optional<Equipment> findById(int id);
    /** Kept for occasional administrative use (not called by the screen) -- the normal flow is deactivate/activate. */
    void remove(int id);
    void deactivate(int id);
    void activate(int id);
}
