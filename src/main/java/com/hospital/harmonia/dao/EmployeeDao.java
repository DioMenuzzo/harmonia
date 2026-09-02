package com.hospital.harmonia.dao;

import com.hospital.harmonia.model.Employee;

import java.util.List;
import java.util.Optional;

public interface EmployeeDao {
    Employee save(Employee employee);
    Optional<Employee> findById(int id);
    /** Used in bulk import to check whether the registration number is already registered. */
    Optional<Employee> findByRegistrationNumber(String registrationNumber);
    List<Employee> findAll();
    List<Employee> findActive();
    void update(Employee employee);
    void deactivate(int id);
    void activate(int id);
}
