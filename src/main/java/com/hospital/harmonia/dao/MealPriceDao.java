package com.hospital.harmonia.dao;

import com.hospital.harmonia.model.MealType;

import java.math.BigDecimal;
import java.util.Map;

public interface MealPriceDao {
    /** Prices configured in the database -- only contains the types saved at least once. */
    Map<MealType, BigDecimal> findAll();

    /** Upsert: creates the row the first time this type's price is configured, updates afterwards. */
    void save(MealType type, BigDecimal price);

    /**
     * Upserts every given type/price pair as a single atomic operation: either
     * all of them are saved or none are (see MealPriceService.savePrices --
     * this is what it calls instead of looping save() one type at a time,
     * which left prices half-updated if the database dropped the connection
     * partway through the loop).
     */
    void saveAll(Map<MealType, BigDecimal> prices);
}
