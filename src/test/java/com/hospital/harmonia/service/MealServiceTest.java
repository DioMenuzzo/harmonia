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

    // --- One meal per type per employee per day (agreed with RH) ---

    @Test
    void register_rejectsASecondMealOfTheSameTypeOnTheSameDay() {
        service.register(validMeal()); // Almoço, 2026-02-10

        Meal secondLunch = validMeal();
        secondLunch.setTime(LocalTime.of(13, 30));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.register(secondLunch));
        assertTrue(ex.getMessage().contains("Almoço"), "message should name the conflicting meal type");
        assertEquals(1, fakeDao.saved.size(), "the second (rejected) lunch must not reach the DAO");
    }

    @Test
    void register_allowsDifferentMealTypesOnTheSameDay() {
        service.register(validMeal()); // Almoço

        Meal dinner = validMeal();
        dinner.setType(MealType.JANTAR);
        dinner.setTime(LocalTime.of(19, 0));
        service.register(dinner);

        assertEquals(2, fakeDao.saved.size());
    }

    @Test
    void register_allowsTheSameMealTypeOnADifferentDay() {
        service.register(validMeal()); // Almoço, 2026-02-10

        Meal nextDayLunch = validMeal();
        nextDayLunch.setDate(LocalDate.of(2026, 2, 11));
        service.register(nextDayLunch);

        assertEquals(2, fakeDao.saved.size());
    }

    @Test
    void register_allowsTheSameMealTypeForADifferentEmployeeOnTheSameDay() {
        service.register(validMeal()); // employeeId=1, Almoço

        Meal otherEmployeeLunch = validMeal();
        otherEmployeeLunch.setEmployeeId(2);
        service.register(otherEmployeeLunch);

        assertEquals(2, fakeDao.saved.size());
    }

    @Test
    void register_allowsAnotherMealOfTheSameTypeOnceTheFirstIsDeactivated() {
        Meal firstLunch = service.register(validMeal());
        service.deactivate(firstLunch.getId());

        Meal secondLunch = validMeal();
        secondLunch.setTime(LocalTime.of(13, 0));
        service.register(secondLunch); // must not throw -- the canceled lunch no longer counts

        assertEquals(2, fakeDao.saved.size());
    }

    @Test
    void update_doesNotConflictWithTheRecordBeingEdited() {
        Meal lunch = service.register(validMeal());
        lunch.setTime(LocalTime.of(12, 15)); // same day, same type -- just correcting the time
        service.update(lunch); // must not throw
        assertEquals(12, fakeDao.saved.get(0).getTime().getHour());
    }

    @Test
    void register_allowsMultipleMealsOfTheSameTypeWhenEmployeeUsesSharedRegistration() {
        // A shared matrícula (e.g. "plantão médico" badge used by whichever
        // doctor is on call) is expected to have several people eating under
        // it on the same day -- the one-meal-per-type-per-day rule must not
        // apply to it (see Employee.sharedUsage / Meal.employeeSharedUsage).
        Meal firstLunch = validMeal();
        firstLunch.setEmployeeSharedUsage(true);
        service.register(firstLunch);

        Meal secondLunch = validMeal();
        secondLunch.setEmployeeSharedUsage(true);
        secondLunch.setTime(LocalTime.of(13, 0));
        service.register(secondLunch); // must not throw

        assertEquals(2, fakeDao.saved.size());
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
        // Mirrors the real DAO assigning an id via Postgres' "RETURNING id"
        // on INSERT -- without this, every saved meal would keep id == null
        // forever, which would let checkNoDuplicateMealType's self-exclusion
        // (by id) silently treat any two different null-id meals as "the
        // same record" instead of catching a real conflict between them.
        private int nextId = 1;

        @Override
        public Meal save(Meal meal) {
            meal.setId(nextId++);
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
