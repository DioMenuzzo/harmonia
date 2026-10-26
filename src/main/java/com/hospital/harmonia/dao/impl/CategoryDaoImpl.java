package com.hospital.harmonia.dao.impl;

import com.hospital.harmonia.config.DatabaseConfig;
import com.hospital.harmonia.dao.CategoryDao;
import com.hospital.harmonia.dao.DataAccessException;
import com.hospital.harmonia.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryDaoImpl implements CategoryDao {

    private static final Logger log = LoggerFactory.getLogger(CategoryDaoImpl.class);

    // Postgres SQLSTATE for "unique_violation" -- see
    // https://www.postgresql.org/docs/current/errcodes-appendix.html
    private static final String SQLSTATE_UNIQUE_VIOLATION = "23505";

    @Override
    public Category save(Category category) {
        String sql = "INSERT INTO categorias (nome, ativo) VALUES (?, TRUE) RETURNING id";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.getName());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    category.setId(rs.getInt(1));
                }
            }
            log.info("Category registered: id={} name={}", category.getId(), category.getName());
            return category;
        } catch (SQLException ex) {
            throw translate(ex, category.getName(), "Erro ao salvar categoria");
        }
    }

    @Override
    public void update(Category category) {
        String sql = "UPDATE categorias SET nome = ?, ativo = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.getName());
            stmt.setBoolean(2, category.isActive());
            stmt.setInt(3, category.getId());
            stmt.executeUpdate();
            log.info("Category updated: id={} name={}", category.getId(), category.getName());
        } catch (SQLException ex) {
            throw translate(ex, category.getName(), "Erro ao atualizar categoria " + category.getId());
        }
    }

    /**
     * The database has a unique index on UPPER(TRIM(nome)), so two spellings
     * that differ only in case or padding can never coexist -- that's what
     * made the same group of people show up as two separate blocks on the
     * report. Turned into a message the user can act on instead of a generic
     * data-access failure (same treatment EmployeeDaoImpl gives a duplicate
     * matrícula).
     */
    private RuntimeException translate(SQLException ex, String name, String fallbackMessage) {
        if (SQLSTATE_UNIQUE_VIOLATION.equals(ex.getSQLState())) {
            log.warn("Attempt to save duplicate category name: {}", name);
            return new IllegalArgumentException("Já existe uma categoria chamada \"" + name + "\".");
        }
        log.error("{} (name={})", fallbackMessage, name, ex);
        return new DataAccessException(fallbackMessage, ex);
    }

    @Override
    public Optional<Category> findById(int id) {
        String sql = "SELECT * FROM categorias WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("Failed to fetch category id={}", id, e);
            throw new DataAccessException("Erro ao buscar categoria " + id, e);
        }
    }

    @Override
    public Optional<Category> findByName(String name) {
        // Same UPPER(TRIM(...)) expression as the unique index, so this lookup
        // uses it and matches exactly what the index considers a duplicate.
        String sql = "SELECT * FROM categorias WHERE UPPER(TRIM(nome)) = UPPER(TRIM(?))";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("Failed to fetch category by name={}", name, e);
            throw new DataAccessException("Erro ao buscar categoria " + name, e);
        }
    }

    @Override
    public List<Category> findAll() {
        return list("SELECT * FROM categorias ORDER BY nome");
    }

    @Override
    public List<Category> findActive() {
        return list("SELECT * FROM categorias WHERE ativo = TRUE ORDER BY nome");
    }

    private List<Category> list(String sql) {
        List<Category> result = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            log.error("Failed to list categories", e);
            throw new DataAccessException("Erro ao listar categorias", e);
        }
    }

    @Override
    public void deactivate(int id) {
        setActive(id, false);
    }

    @Override
    public void activate(int id) {
        setActive(id, true);
    }

    private void setActive(int id, boolean active) {
        // Soft delete, like employees and meals: a category still referenced by
        // employees (and therefore by past reports) is never physically removed.
        String sql = "UPDATE categorias SET ativo = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, active);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            log.info("Category {}: id={}", active ? "activated" : "deactivated", id);
        } catch (SQLException e) {
            String message = (active ? "Erro ao ativar categoria " : "Erro ao inativar categoria ") + id;
            log.error(message, e);
            throw new DataAccessException(message, e);
        }
    }

    @Override
    public long countEmployees(int categoryId) {
        String sql = "SELECT count(*) FROM colaboradores WHERE categoria_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            log.error("Failed to count employees of category id={}", categoryId, e);
            throw new DataAccessException("Erro ao contar colaboradores da categoria " + categoryId, e);
        }
    }

    private Category map(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setId(rs.getInt("id"));
        category.setName(rs.getString("nome"));
        category.setActive(rs.getBoolean("ativo"));
        return category;
    }
}
