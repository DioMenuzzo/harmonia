package com.hospital.harmonia.dao;

import com.hospital.harmonia.model.MealType;

import java.math.BigDecimal;
import java.util.Map;

public interface MealPriceDao {
    /** Prices configured in the database -- only contains the types saved at least once. */
    Map<MealType, BigDecimal> findAll();

    /** Upsert: creates the row the first time this type's price is configured, updates afterwards. */
    void save(MealType type, BigDecimal price);
}
