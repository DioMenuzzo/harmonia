package com.hospital.harmonia.dao.impl;

import com.hospital.harmonia.config.DatabaseConfig;
import com.hospital.harmonia.dao.ContactDao;
import com.hospital.harmonia.dao.DataAccessException;
import com.hospital.harmonia.model.Contact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

// The category lives in its own table ("categorias_agenda"), so what this
// DAO writes is categoria_id and what it reads back is the joined name --
// same pattern as EmployeeDaoImpl/"categorias".
public class ContactDaoImpl implements ContactDao {

    private static final Logger log = LoggerFactory.getLogger(ContactDaoImpl.class);

    // LEFT JOIN (not INNER): a contact with no category must still be
    // listed, with categoria_nome coming back NULL.
    private static final String SELECT_WITH_CATEGORY =
            "SELECT c.*, cat.nome AS categoria_nome FROM contatos_agenda c " +
            "LEFT JOIN categorias_agenda cat ON cat.id = c.categoria_id";

    @Override
    public Contact save(Contact contact) {
        String sql = "INSERT INTO contatos_agenda (nome, categoria_id, celular, telefone, email, observacao) " +
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            fill(stmt, contact);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    contact.setId(rs.getInt(1));
                }
            }
            log.info("Contact registered: id={} name={}", contact.getId(), contact.getName());
            return contact;
        } catch (SQLException e) {
            log.error("Failed to save contact (name={})", contact.getName(), e);
            throw new DataAccessException("Erro ao salvar contato", e);
        }
    }

    @Override
    public void update(Contact contact) {
        String sql = "UPDATE contatos_agenda SET nome=?, categoria_id=?, celular=?, telefone=?, email=?, observacao=? WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            fill(stmt, contact);
            stmt.setInt(7, contact.getId());
            stmt.executeUpdate();
            log.info("Contact updated: id={}", contact.getId());
        } catch (SQLException e) {
            log.error("Failed to update contact id={}", contact.getId(), e);
            throw new DataAccessException("Erro ao atualizar contato " + contact.getId(), e);
        }
    }

    private void fill(PreparedStatement stmt, Contact contact) throws SQLException {
        stmt.setString(1, contact.getName());
        if (contact.getCategoryId() == null) {
            stmt.setNull(2, Types.INTEGER);
        } else {
            stmt.setInt(2, contact.getCategoryId());
        }
        stmt.setString(3, contact.getCellPhone());
        stmt.setString(4, contact.getPhone());
        stmt.setString(5, contact.getEmail());
        stmt.setString(6, contact.getNote());
    }

    @Override
    public List<Contact> findAll() {
        return list(SELECT_WITH_CATEGORY + " ORDER BY c.nome");
    }

    @Override
    public Optional<Contact> findById(int id) {
        String sql = SELECT_WITH_CATEGORY + " WHERE c.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("Failed to fetch contact id={}", id, e);
            throw new DataAccessException("Erro ao buscar contato " + id, e);
        }
    }

    @Override
    public void remove(int id) {
        // Kept for occasional administrative use (not called by the screen) --
        // the normal flow is deactivate/activate, which preserves the record for the log.
        String sql = "DELETE FROM contatos_agenda WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            log.warn("Contact permanently removed: id={}", id);
        } catch (SQLException e) {
            log.error("Failed to remove contact id={}", id, e);
            throw new DataAccessException("Erro ao excluir contato " + id, e);
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
        // Soft delete, like employees/meals/equipment: a contact is never
        // physically removed by the normal flow, just hidden from the active list.
        String sql = "UPDATE contatos_agenda SET ativo = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, active);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            log.info("Contact {}: id={}", active ? "activated" : "deactivated", id);
        } catch (SQLException e) {
            String message = (active ? "Erro ao ativar contato " : "Erro ao inativar contato ") + id;
            log.error(message, e);
            throw new DataAccessException(message, e);
        }
    }

    private List<Contact> list(String sql) {
        return JdbcListSupport.queryList(log, sql, this::map,
                "Failed to list contacts", "Erro ao listar contatos");
    }

    private Contact map(ResultSet rs) throws SQLException {
        Contact contact = new Contact();
        contact.setId(rs.getInt("id"));
        contact.setName(rs.getString("nome"));
        // getInt returns 0 for SQL NULL, so wasNull() is what tells a contact
        // with no category apart from one linked to category id 0.
        int categoryId = rs.getInt("categoria_id");
        contact.setCategoryId(rs.wasNull() ? null : categoryId);
        contact.setCategoryName(rs.getString("categoria_nome"));
        contact.setCellPhone(rs.getString("celular"));
        contact.setPhone(rs.getString("telefone"));
        contact.setEmail(rs.getString("email"));
        contact.setNote(rs.getString("observacao"));
        contact.setActive(rs.getBoolean("ativo"));
        return contact;
    }
}
