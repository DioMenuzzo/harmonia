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

    private final MealPriceDao mealPriceDao = new MealPriceDaoImpl();

    /** Current price (configured or default) of each type -- used to fill the screen. */
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

    /** Saves (upserts) the price of each given type. None can be negative. */
    public void savePrices(Map<MealType, BigDecimal> prices) {
        for (Map.Entry<MealType, BigDecimal> entry : prices.entrySet()) {
            BigDecimal price = entry.getValue();
            if (price == null || price.signum() < 0) {
                throw new IllegalArgumentException(
                        "Preço de " + entry.getKey().getDescription() + " inválido.");
            }
        }
        for (Map.Entry<MealType, BigDecimal> entry : prices.entrySet()) {
            mealPriceDao.save(entry.getKey(), entry.getValue());
        }
        log.debug("Meal prices saved for {} type(s).", prices.size());
    }
}
