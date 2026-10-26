package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.MealPriceDao;
import com.hospital.harmonia.dao.impl.MealPriceDaoImpl;
import com.hospital.harmonia.model.MealType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Meal prices, configurable from the screen (Reports tab) instead of fixed in
 * code. A type not yet configured in the database falls back to the
 * PLACEHOLDER value from MealType.getDefaultPrice() -- so the system keeps
 * working normally even before the first configuration.
 */
public class MealPriceService {

    private static final Logger log = LoggerFactory.getLogger(MealPriceService.class);

    private final MealPriceDao mealPriceDao;

    public MealPriceService() {
        this(new MealPriceDaoImpl());
    }

    /** Package-visible constructor allowing a test double to be injected (see MealPriceServiceTest). */
    MealPriceService(MealPriceDao mealPriceDao) {
        this.mealPriceDao = mealPriceDao;
    }

    /**
     * Current price (configured or default) of each type -- used to fill the screen.
     *
     * Deliberately NOT named findAll(), unlike the equivalent "get everything"
     * method on the other services: this merges the DAO's findAll() with
     * MealType.getDefaultPrice() for whatever hasn't been configured yet (see
     * below), so it never behaves like a plain passthrough the way
     * EmployeeService.findAll() and UserService.findAll() do -- calling it
     * findAll() would suggest it just relays MealPriceDao.findAll(), which
     * only returns the types actually configured in the database.
     */
    public Map<MealType, BigDecimal> listPrices() {
        Map<MealType, BigDecimal> configured = mealPriceDao.findAll();
        Map<MealType, BigDecimal> prices = new EnumMap<>(MealType.class);
        for (MealType type : MealType.values()) {
            prices.put(type, configured.getOrDefault(type, type.getDefaultPrice()));
        }
        return prices;
    }

    /** Current price of a single type -- used when registering/importing a single meal. */
    public BigDecimal getPrice(MealType type) {
        return mealPriceDao.findAll().getOrDefault(type, type.getDefaultPrice());
    }

    /**
     * Saves (upserts) the price of each given type, all as one atomic
     * operation (see MealPriceDao.saveAll) -- either every type's new price
     * takes effect or none does, instead of the screen's "save" possibly
     * leaving some types priced under the new table and others still under
     * the old one. None can be negative.
     */
    public void savePrices(Map<MealType, BigDecimal> prices) {
        for (Map.Entry<MealType, BigDecimal> entry : prices.entrySet()) {
            BigDecimal price = entry.getValue();
            if (price == null || price.signum() < 0) {
                throw new IllegalArgumentException(
                        "Preço de " + entry.getKey().getDescription() + " inválido.");
            }
        }
        mealPriceDao.saveAll(prices);
        log.debug("Meal prices saved for {} type(s).", prices.size());
    }
}
