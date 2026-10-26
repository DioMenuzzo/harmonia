package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.MealPriceDao;
import com.hospital.harmonia.model.MealType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Exercises MealPriceService's validation and, importantly, that
 * savePrices() saves every type through ONE call to MealPriceDao.saveAll
 * instead of looping save() one type at a time -- see saveAll's Javadoc and
 * MealPriceDaoImpl.saveAll: the per-type loop this replaced could commit some
 * types' new prices and not others if the database dropped the connection
 * partway through. FakeMealPriceDao below tracks calls to both methods
 * precisely so a regression back to that loop fails saveAllOncePerCall().
 */
class MealPriceServiceTest {

    private FakeMealPriceDao fakeDao;
    private MealPriceService service;

    @BeforeEach
    void setUp() {
        fakeDao = new FakeMealPriceDao();
        service = new MealPriceService(fakeDao);
    }

    @Test
    void listPrices_fillsInTheDefaultForATypeNeverConfigured() {
        Map<MealType, BigDecimal> prices = service.listPrices();
        assertEquals(MealType.ALMOCO.getDefaultPrice(), prices.get(MealType.ALMOCO));
    }

    @Test
    void listPrices_usesTheConfiguredPriceInsteadOfTheDefaultWhenPresent() {
        fakeDao.configured.put(MealType.ALMOCO, new BigDecimal("22.50"));
        assertEquals(new BigDecimal("22.50"), service.listPrices().get(MealType.ALMOCO));
    }

    @Test
    void listPrices_alwaysReturnsEveryType() {
        assertEquals(MealType.values().length, service.listPrices().size());
    }

    @Test
    void getPrice_returnsTheConfiguredPriceWhenPresent() {
        fakeDao.configured.put(MealType.JANTAR, new BigDecimal("16.00"));
        assertEquals(new BigDecimal("16.00"), service.getPrice(MealType.JANTAR));
    }

    @Test
    void getPrice_fallsBackToTheDefaultWhenNotConfigured() {
        assertEquals(MealType.CEIA.getDefaultPrice(), service.getPrice(MealType.CEIA));
    }

    @Test
    void savePrices_rejectsANullPrice() {
        Map<MealType, BigDecimal> prices = new EnumMap<>(MealType.class);
        prices.put(MealType.ALMOCO, null);
        assertThrows(IllegalArgumentException.class, () -> service.savePrices(prices));
        assertEquals(0, fakeDao.saveAllCallCount, "an invalid entry must stop the whole save, not just skip that entry");
    }

    @Test
    void savePrices_rejectsANegativePrice() {
        Map<MealType, BigDecimal> prices = new EnumMap<>(MealType.class);
        prices.put(MealType.JANTAR, new BigDecimal("-1.00"));
        assertThrows(IllegalArgumentException.class, () -> service.savePrices(prices));
    }

    @Test
    void savePrices_acceptsAZeroPrice() {
        Map<MealType, BigDecimal> prices = new EnumMap<>(MealType.class);
        prices.put(MealType.LANCHE, BigDecimal.ZERO);
        service.savePrices(prices);
        assertEquals(BigDecimal.ZERO, fakeDao.configured.get(MealType.LANCHE));
    }

    @Test
    void savePrices_ifOneTypeIsInvalidNoneOfTheOthersAreSavedEither() {
        Map<MealType, BigDecimal> prices = new EnumMap<>(MealType.class);
        prices.put(MealType.ALMOCO, new BigDecimal("20.00"));
        prices.put(MealType.JANTAR, new BigDecimal("-5.00")); // invalid
        assertThrows(IllegalArgumentException.class, () -> service.savePrices(prices));
        assertEquals(0, fakeDao.saveAllCallCount);
        assertEquals(0, fakeDao.saveCallCount);
    }

    @Test
    void savePrices_savesEveryTypeThroughOneAtomicCallNotOnePerType() {
        Map<MealType, BigDecimal> prices = new EnumMap<>(MealType.class);
        prices.put(MealType.CEIA, new BigDecimal("11.00"));
        prices.put(MealType.CAFE_DA_MANHA, new BigDecimal("9.00"));
        prices.put(MealType.ALMOCO, new BigDecimal("19.00"));
        prices.put(MealType.JANTAR, new BigDecimal("16.00"));

        service.savePrices(prices);

        assertEquals(1, fakeDao.saveAllCallCount, "all four types must go through a single saveAll() call");
        assertEquals(0, fakeDao.saveCallCount, "must not fall back to the old per-type save() loop");
        assertEquals(4, fakeDao.configured.size());
        assertEquals(new BigDecimal("19.00"), fakeDao.configured.get(MealType.ALMOCO));
    }

    /** Minimal in-memory stand-in for MealPriceDaoImpl -- no database involved. */
    private static class FakeMealPriceDao implements MealPriceDao {
        final Map<MealType, BigDecimal> configured = new EnumMap<>(MealType.class);
        int saveCallCount = 0;
        int saveAllCallCount = 0;

        @Override
        public Map<MealType, BigDecimal> findAll() {
            return new EnumMap<>(configured);
        }

        @Override
        public void save(MealType type, BigDecimal price) {
            saveCallCount++;
            configured.put(type, price);
        }

        @Override
        public void saveAll(Map<MealType, BigDecimal> prices) {
            saveAllCallCount++;
            configured.putAll(prices);
        }
    }
}
