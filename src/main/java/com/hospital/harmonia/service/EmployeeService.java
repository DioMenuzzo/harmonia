package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.EmployeeDao;
import com.hospital.harmonia.dao.impl.EmployeeDaoImpl;
import com.hospital.harmonia.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeDao employeeDao;

    public EmployeeService() {
        this(new EmployeeDaoImpl());
    }

    /** Package-visible constructor allowing a test double to be injected (see EmployeeServiceTest). */
    EmployeeService(EmployeeDao employeeDao) {
        this.employeeDao = employeeDao;
    }

    public Employee register(Employee employee) {
        validate(employee);
        log.debug("Registering employee: registrationNumber={}", employee.getRegistrationNumber());
        return employeeDao.save(employee);
    }

    /** Used in bulk import to check whether the registration number is already registered. */
    public Optional<Employee> findByRegistrationNumber(String registrationNumber) {
        return employeeDao.findByRegistrationNumber(registrationNumber);
    }

    public void update(Employee employee) {
        validate(employee);
        employeeDao.update(employee);
    }

    public void deactivate(int id) {
        employeeDao.deactivate(id);
    }

    public void activate(int id) {
        employeeDao.activate(id);
    }

    public List<Employee> findActive() {
        return employeeDao.findActive();
    }

    public List<Employee> findAll() {
        return employeeDao.findAll();
    }

    private void validate(Employee e) {
        if (e.getName() == null || e.getName().isBlank()) {
            throw new IllegalArgumentException("Nome do colaborador é obrigatório.");
        }
        if (e.getRegistrationNumber() == null || e.getRegistrationNumber().isBlank()) {
            throw new IllegalArgumentException("CPF do colaborador é obrigatório.");
        }
    }
}
