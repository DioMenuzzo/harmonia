package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.EmployeeDao;
import com.hospital.harmonia.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises EmployeeService's validation rules in isolation, with an
 * in-memory fake instead of a real database (see FakeEmployeeDao below).
 * EmployeeService accepts an injected EmployeeDao through its
 * package-private constructor exactly so it can be tested this way.
 */
class EmployeeServiceTest {

    private FakeEmployeeDao fakeDao;
    private EmployeeService service;

    @BeforeEach
    void setUp() {
        fakeDao = new FakeEmployeeDao();
        service = new EmployeeService(fakeDao);
    }

    @Test
    void register_rejectsABlankName() {
        Employee employee = new Employee(null, "  ", "00012345", "ALUNOS", null);
        assertThrows(IllegalArgumentException.class, () -> service.register(employee));
        assertTrue(fakeDao.saved.isEmpty(), "must not reach the DAO when validation fails");
    }

    @Test
    void register_rejectsABlankRegistrationNumber() {
        Employee employee = new Employee(null, "Maria Clara Oliveira", " ", "ALUNOS", null);
        assertThrows(IllegalArgumentException.class, () -> service.register(employee));
        assertTrue(fakeDao.saved.isEmpty());
    }

    @Test
    void register_savesAValidEmployeeThroughTheDao() {
        Employee employee = new Employee(null, "Maria Clara Oliveira", "00012345", "ALUNOS", null);
        service.register(employee);
        assertEquals(1, fakeDao.saved.size());
        assertEquals("Maria Clara Oliveira", fakeDao.saved.get(0).getName());
    }

    /** Minimal in-memory stand-in for EmployeeDaoImpl -- no database involved. */
    private static class FakeEmployeeDao implements EmployeeDao {
        final List<Employee> saved = new ArrayList<>();

        @Override
        public Employee save(Employee employee) {
            saved.add(employee);
            return employee;
        }

        @Override
        public Optional<Employee> findById(int id) {
            return saved.stream().filter(e -> Integer.valueOf(id).equals(e.getId())).findFirst();
        }

        @Override
        public Optional<Employee> findByRegistrationNumber(String registrationNumber) {
            return saved.stream().filter(e -> e.getRegistrationNumber().equals(registrationNumber)).findFirst();
        }

        @Override
        public List<Employee> findAll() {
            return saved;
        }

        @Override
        public List<Employee> findActive() {
            return saved.stream().filter(Employee::isActive).toList();
        }

        @Override
        public void update(Employee employee) {
            saved.removeIf(e -> Integer.valueOf(employee.getId()).equals(e.getId()));
            saved.add(employee);
        }

        @Override
        public void deactivate(int id) {
            findById(id).ifPresent(e -> e.setActive(false));
        }

        @Override
        public void activate(int id) {
            findById(id).ifPresent(e -> e.setActive(true));
        }
    }
}
