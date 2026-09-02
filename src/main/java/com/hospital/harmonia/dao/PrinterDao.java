package com.hospital.harmonia.dao;

import com.hospital.harmonia.model.Printer;

import java.util.List;

public interface PrinterDao {
    Printer save(Printer printer);
    void update(Printer printer);
    List<Printer> findAll();
}
