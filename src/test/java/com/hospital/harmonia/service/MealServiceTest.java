package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.MealDao;
import com.hospital.harmonia.model.Meal;
import com.hospital.harmonia.model.MealType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises MealService's validation rules and total-spent calculation in
 * isolation, with an in-memory fake instead of a real database (see
 * FakeMealDao below). MealService accepts an injected MealDao through its
 * package-private constructor exactly so it can be tested this way.
 */
class MealServiceTest {

    private FakeMealDao fakeDao;
    private MealService service;

    @BeforeEach
    void setUp() {
        fakeDao = new FakeMealDao();
        service = new MealService(fakeDao);
    }

    private static Meal validMeal() {
        Meal meal = new Meal();
        meal.setEmployeeId(1);
        meal.setDate(LocalDate.of(2026, 2, 10));
        meal.setTime(LocalTime.of(12, 0));
        meal.setPrice(new BigDecimal("18.00"));
        meal.setType(MealType.ALMOCO);
        return meal;
    }

    @Test
    void register_rejectsAMealWithoutAnEmployee() {
        Meal meal = validMeal();
        meal.setEmployeeId(null);
        assertThrows(IllegalArgumentException.class, () -> service.register(meal));
    }

    @Test
    void register_rejectsAMealWithoutADate() {
        Meal meal = validMeal();
        meal.setDate(null);
        assertThrows(IllegalArgumentException.class, () -> service.register(meal));
    }

    @Test
    void register_rejectsANegativePrice() {
        Meal meal = validMeal();
        meal.setPrice(new BigDecimal("-1.00"));
        assertThrows(IllegalArgumentException.class, () -> service.register(meal));
    }

    @Test
    void register_rejectsAMealWithoutAType() {
        Meal meal = validMeal();
        meal.setType(null);
        assertThrows(IllegalArgumentException.class, () -> service.register(meal));
    }

    @Test
    void register_savesAValidMealThroughTheDao() {
        service.register(validMeal());
        assertEquals(1, fakeDao.saved.size());
    }

    @Test
    void findByPeriod_rejectsAStartDateAfterTheEndDate() {
        LocalDate start = LocalDate.of(2026, 2, 10);
        LocalDate end = LocalDate.of(2026, 2, 1);
        assertThrows(IllegalArgumentException.class, () -> service.findByPeriod(start, end));
    }

    @Test
    void totalForPeriod_sumsThePriceOfEveryMeal() {
        Meal breakfast = validMeal();
        breakfast.setPrice(new BigDecimal("8.00"));
        Meal lunch = validMeal();
        lunch.setPrice(new BigDecimal("18.50"));

        BigDecimal total = service.totalForPeriod(List.of(breakfast, lunch));

        assertTrue(new BigDecimal("26.50").compareTo(total) == 0);
    }

    @Test
    void totalForPeriod_isZeroForAnEmptyList() {
        assertTrue(BigDecimal.ZERO.compareTo(service.totalForPeriod(List.of())) == 0);
    }

    /** Minimal in-memory stand-in for MealDaoImpl -- no database involved. */
    private static class FakeMealDao implements MealDao {
        final List<Meal> saved = new ArrayList<>();

        @Override
        public Meal save(Meal meal) {
            saved.add(meal);
            return meal;
        }

        @Override
        public void update(Meal meal) {
            saved.removeIf(m -> Integer.valueOf(meal.getId()).equals(m.getId()));
            saved.add(meal);
        }

        @Override
        public void remove(int id) {
            saved.removeIf(m -> Integer.valueOf(id).equals(m.getId()));
        }

        @Override
        public void deactivate(int id) {
            saved.stream().filter(m -> Integer.valueOf(id).equals(m.getId())).findFirst()
                    .ifPresent(m -> m.setActive(false));
        }

        @Override
        public void activate(int id) {
            saved.stream().filter(m -> Integer.valueOf(id).equals(m.getId())).findFirst()
                    .ifPresent(m -> m.setActive(true));
        }

        @Override
        public List<Meal> findByPeriod(LocalDate start, LocalDate end) {
            return saved.stream()
                    .filter(m -> !m.getDate().isBefore(start) && !m.getDate().isAfter(end))
                    .toList();
        }

        @Override
        public List<Meal> findActiveByPeriod(LocalDate start, LocalDate end) {
            return findByPeriod(start, end).stream().filter(Meal::isActive).toList();
        }

        @Override
        public List<Meal> findByEmployeeAndPeriod(int employeeId, LocalDate start, LocalDate end) {
            return findByPeriod(start, end).stream()
                    .filter(m -> Integer.valueOf(employeeId).equals(m.getEmployeeId()))
                    .toList();
        }
    }
}
