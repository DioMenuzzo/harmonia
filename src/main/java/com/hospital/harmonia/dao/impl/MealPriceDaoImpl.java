package com.hospital.harmonia.dao.impl;

import com.hospital.harmonia.config.DatabaseConfig;
import com.hospital.harmonia.dao.DataAccessException;
import com.hospital.harmonia.dao.MealPriceDao;
import com.hospital.harmonia.model.MealType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumMap;
import java.util.Map;

// NOTE: table/column names (precos_refeicao, tipo, preco) are still the ones
// actually in the database -- only the Java-side names were translated.
public class MealPriceDaoImpl implements MealPriceDao {

    private static final Logger log = LoggerFactory.getLogger(MealPriceDaoImpl.class);

    @Override
    public Map<MealType, BigDecimal> findAll() {
        Map<MealType, BigDecimal> prices = new EnumMap<>(MealType.class);
        String sql = "SELECT tipo, preco FROM precos_refeicao";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                prices.put(MealType.valueOf(rs.getString("tipo")), rs.getBigDecimal("preco"));
            }
            return prices;
        } catch (SQLException e) {
            log.error("Failed to list meal prices", e);
            throw new DataAccessException("Erro ao listar precos de refeicao", e);
        }
    }

    @Override
    public void save(MealType type, BigDecimal price) {
        // Upsert: creates the row the first time this type's price is
        // configured through the screen, updates it afterwards.
        String sql = "INSERT INTO precos_refeicao (tipo, preco) VALUES (?, ?) " +
                "ON CONFLICT (tipo) DO UPDATE SET preco = EXCLUDED.preco";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type.name());
            stmt.setBigDecimal(2, price);
            stmt.executeUpdate();
            log.info("Meal price updated: type={} price={}", type, price);
        } catch (SQLException e) {
            log.error("Failed to save price for type={}", type, e);
            throw new DataAccessException("Erro ao salvar preco de " + type, e);
        }
    }
}
