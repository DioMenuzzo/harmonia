package com.hospital.harmonia.dao.impl;

import com.hospital.harmonia.config.DatabaseConfig;
import com.hospital.harmonia.dao.DataAccessException;
import com.hospital.harmonia.dao.EmployeeDao;
import com.hospital.harmonia.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

// NOTE: table/column names (colaboradores, nome, cpf, cargo, ativo) are still
// the ones actually in the database -- only the Java-side names were
// translated. Update these SQL strings together with the database migration
// when that happens.
//
// The category lives in its own table now ("categorias"), so what this DAO
// writes is categoria_id and what it reads back is the joined name. The old
// free-text column empresa_terceirizada is deliberately left untouched in the
// database as a backup of the pre-migration values -- nothing here reads or
// writes it any more, and it can be dropped once the migration is trusted.
public class EmployeeDaoImpl implements EmployeeDao {

    private static final Logger log = LoggerFactory.getLogger(EmployeeDaoImpl.class);

    // Postgres SQLSTATE for "unique_violation" -- see
    // https://www.postgresql.org/docs/current/errcodes-appendix.html
    private static final String SQLSTATE_UNIQUE_VIOLATION = "23505";

    // LEFT JOIN (not INNER): an employee with no category must still be
    // listed, with categoria_nome coming back NULL.
    private static final String SELECT_WITH_CATEGORY =
            "SELECT c.*, cat.nome AS categoria_nome FROM colaboradores c " +
            "LEFT JOIN categorias cat ON cat.id = c.categoria_id";

    /** Writes the category link, or SQL NULL when the employee has no category assigned. */
    private static void setCategoryId(PreparedStatement stmt, int parameterIndex, Integer categoryId)
            throws SQLException {
        if (categoryId == null) {
            stmt.setNull(parameterIndex, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(parameterIndex, categoryId);
        }
    }

    @Override
    public Employee save(Employee e) {
        String sql = "INSERT INTO colaboradores (nome, cpf, categoria_id, cargo, uso_compartilhado, ativo) " +
                "VALUES (?, ?, ?, ?, ?, TRUE) RETURNING id";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, e.getName());
            stmt.setString(2, e.getRegistrationNumber());
            setCategoryId(stmt, 3, e.getCategoryId());
            stmt.setString(4, e.getJobTitle());
            stmt.setBoolean(5, e.isSharedUsage());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    e.setId(rs.getInt(1));
                }
            }
            log.info("Employee registered: id={} registrationNumber={}", e.getId(), e.getRegistrationNumber());
            return e;
        } catch (SQLException ex) {
            if (SQLSTATE_UNIQUE_VIOLATION.equals(ex.getSQLState())) {
                log.warn("Attempt to register duplicate registration number: {}", e.getRegistrationNumber());
                throw new IllegalArgumentException(
                        "Já existe um colaborador cadastrado com a matrícula " + e.getRegistrationNumber() + ".");
            }
            log.error("Failed to save employee (registrationNumber={})", e.getRegistrationNumber(), ex);
            throw new DataAccessException("Erro ao salvar colaborador", ex);
        }
    }

    @Override
    public Optional<Employee> findById(int id) {
        String sql = SELECT_WITH_CATEGORY + " WHERE c.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("Failed to fetch employee id={}", id, e);
            throw new DataAccessException("Erro ao buscar colaborador " + id, e);
        }
    }

    @Override
    public Optional<Employee> findByRegistrationNumber(String registrationNumber) {
        String sql = SELECT_WITH_CATEGORY + " WHERE c.cpf = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, registrationNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("Failed to fetch employee by registration number={}", registrationNumber, e);
            throw new DataAccessException("Erro ao buscar colaborador por matrícula " + registrationNumber, e);
        }
    }

    @Override
    public List<Employee> findAll() {
        return list(SELECT_WITH_CATEGORY + " ORDER BY c.nome");
    }

    @Override
    public List<Employee> findActive() {
        return list(SELECT_WITH_CATEGORY + " WHERE c.ativo = TRUE ORDER BY c.nome");
    }

    private List<Employee> list(String sql) {
        return JdbcListSupport.queryList(log, sql, this::map,
                "Failed to list employees", "Erro ao listar colaboradores");
    }

    @Override
    public void update(Employee e) {
        String sql = "UPDATE colaboradores SET nome = ?, cpf = ?, categoria_id = ?, cargo = ?, " +
                "uso_compartilhado = ?, ativo = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, e.getName());
            stmt.setString(2, e.getRegistrationNumber());
            setCategoryId(stmt, 3, e.getCategoryId());
            stmt.setString(4, e.getJobTitle());
            stmt.setBoolean(5, e.isSharedUsage());
            stmt.setBoolean(6, e.isActive());
            stmt.setInt(7, e.getId());
            stmt.executeUpdate();
            log.info("Employee updated: id={}", e.getId());
        } catch (SQLException ex) {
            if (SQLSTATE_UNIQUE_VIOLATION.equals(ex.getSQLState())) {
                log.warn("Attempt to update employee id={} to duplicate registration number: {}",
                        e.getId(), e.getRegistrationNumber());
                throw new IllegalArgumentException(
                        "Já existe outro colaborador cadastrado com a matrícula " + e.getRegistrationNumber() + ".");
            }
            log.error("Failed to update employee id={}", e.getId(), ex);
            throw new DataAccessException("Erro ao atualizar colaborador " + e.getId(), ex);
        }
    }

    @Override
    public void deactivate(int id) {
        // Soft delete: we never physically remove an employee that has linked
        // meals (referential integrity + history for auditing).
        String sql = "UPDATE colaboradores SET ativo = FALSE WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            log.info("Employee deactivated: id={}", id);
        } catch (SQLException e) {
            log.error("Failed to deactivate employee id={}", id, e);
            throw new DataAccessException("Erro ao inativar colaborador " + id, e);
        }
    }

    @Override
    public void activate(int id) {
        String sql = "UPDATE colaboradores SET ativo = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            log.info("Employee activated: id={}", id);
        } catch (SQLException e) {
            log.error("Failed to activate employee id={}", id, e);
            throw new DataAccessException("Erro ao ativar colaborador " + id, e);
        }
    }

    private Employee map(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setId(rs.getInt("id"));
        e.setName(rs.getString("nome"));
        e.setRegistrationNumber(rs.getString("cpf"));
        // getInt returns 0 for SQL NULL, so wasNull() is what tells an
        // employee with no category apart from one linked to category id 0.
        int categoryId = rs.getInt("categoria_id");
        e.setCategoryId(rs.wasNull() ? null : categoryId);
        e.setCategoryName(rs.getString("categoria_nome"));
        e.setJobTitle(rs.getString("cargo"));
        e.setSharedUsage(rs.getBoolean("uso_compartilhado"));
        e.setActive(rs.getBoolean("ativo"));
        return e;
    }
}
