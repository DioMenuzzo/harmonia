package com.hospital.harmonia.dao.impl;

import com.hospital.harmonia.config.DatabaseConfig;
import com.hospital.harmonia.dao.ContactCategoryDao;
import com.hospital.harmonia.dao.DataAccessException;
import com.hospital.harmonia.model.ContactCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Mirrors CategoryDaoImpl -- see ContactCategory's Javadoc for why this is a separate table/registry. */
public class ContactCategoryDaoImpl implements ContactCategoryDao {

    private static final Logger log = LoggerFactory.getLogger(ContactCategoryDaoImpl.class);

    // Postgres SQLSTATE for "unique_violation" -- see
    // https://www.postgresql.org/docs/current/errcodes-appendix.html
    private static final String SQLSTATE_UNIQUE_VIOLATION = "23505";

    @Override
    public ContactCategory save(ContactCategory category) {
        String sql = "INSERT INTO categorias_agenda (nome, ativo) VALUES (?, TRUE) RETURNING id";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.getName());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    category.setId(rs.getInt(1));
                }
            }
            log.info("Contact category registered: id={} name={}", category.getId(), category.getName());
            return category;
        } catch (SQLException ex) {
            throw translate(ex, category.getName(), "Erro ao salvar categoria");
        }
    }

    @Override
    public void update(ContactCategory category) {
        String sql = "UPDATE categorias_agenda SET nome = ?, ativo = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.getName());
            stmt.setBoolean(2, category.isActive());
            stmt.setInt(3, category.getId());
            stmt.executeUpdate();
            log.info("Contact category updated: id={} name={}", category.getId(), category.getName());
        } catch (SQLException ex) {
            throw translate(ex, category.getName(), "Erro ao atualizar categoria " + category.getId());
        }
    }

    /** Same treatment CategoryDaoImpl gives a duplicate employee category name. */
    private RuntimeException translate(SQLException ex, String name, String fallbackMessage) {
        if (SQLSTATE_UNIQUE_VIOLATION.equals(ex.getSQLState())) {
            log.warn("Attempt to save duplicate contact category name: {}", name);
            return new IllegalArgumentException("Já existe uma categoria chamada \"" + name + "\".");
        }
        log.error("{} (name={})", fallbackMessage, name, ex);
        return new DataAccessException(fallbackMessage, ex);
    }

    @Override
    public Optional<ContactCategory> findById(int id) {
        String sql = "SELECT * FROM categorias_agenda WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("Failed to fetch contact category id={}", id, e);
            throw new DataAccessException("Erro ao buscar categoria " + id, e);
        }
    }

    @Override
    public Optional<ContactCategory> findByName(String name) {
        String sql = "SELECT * FROM categorias_agenda WHERE UPPER(TRIM(nome)) = UPPER(TRIM(?))";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("Failed to fetch contact category by name={}", name, e);
            throw new DataAccessException("Erro ao buscar categoria " + name, e);
        }
    }

    @Override
    public List<ContactCategory> findAll() {
        return list("SELECT * FROM categorias_agenda ORDER BY nome");
    }

    @Override
    public List<ContactCategory> findActive() {
        return list("SELECT * FROM categorias_agenda WHERE ativo = TRUE ORDER BY nome");
    }

    private List<ContactCategory> list(String sql) {
        return JdbcListSupport.queryList(log, sql, this::map,
                "Failed to list contact categories", "Erro ao listar categorias");
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
        String sql = "UPDATE categorias_agenda SET ativo = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, active);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            log.info("Contact category {}: id={}", active ? "activated" : "deactivated", id);
        } catch (SQLException e) {
            String message = (active ? "Erro ao ativar categoria " : "Erro ao inativar categoria ") + id;
            log.error(message, e);
            throw new DataAccessException(message, e);
        }
    }

    @Override
    public long countContacts(int categoryId) {
        String sql = "SELECT count(*) FROM contatos_agenda WHERE categoria_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            log.error("Failed to count contacts of category id={}", categoryId, e);
            throw new DataAccessException("Erro ao contar contatos da categoria " + categoryId, e);
        }
    }

    private ContactCategory map(ResultSet rs) throws SQLException {
        ContactCategory category = new ContactCategory();
        category.setId(rs.getInt("id"));
        category.setName(rs.getString("nome"));
        category.setActive(rs.getBoolean("ativo"));
        return category;
    }
}
