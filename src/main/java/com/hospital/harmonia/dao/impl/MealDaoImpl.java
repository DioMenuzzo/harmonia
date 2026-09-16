package com.hospital.harmonia.dao.impl;

import com.hospital.harmonia.config.DatabaseConfig;
import com.hospital.harmonia.dao.DataAccessException;
import com.hospital.harmonia.dao.MealDao;
import com.hospital.harmonia.model.Meal;
import com.hospital.harmonia.model.MealType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// NOTE: table/column names (refeicoes, colaborador_id, data, horario, preco,
// tipo, ativo) are still the ones actually in the database -- only the
// Java-side names were translated. Update these SQL strings together with the
// database migration when that happens.
public class MealDaoImpl implements MealDao {

    private static final Logger log = LoggerFactory.getLogger(MealDaoImpl.class);

    @Override
    public Meal save(Meal m) {
        String sql = "INSERT INTO refeicoes (colaborador_id, data, horario, preco, tipo) " +
                "VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, m.getEmployeeId());
            stmt.setDate(2, Date.valueOf(m.getDate()));
            stmt.setTime(3, Time.valueOf(m.getTime()));
            stmt.setBigDecimal(4, m.getPrice());
            stmt.setString(5, m.getType().name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    m.setId(rs.getInt(1));
                }
            }
            log.info("Meal registered: id={} employeeId={} date={} type={}",
                    m.getId(), m.getEmployeeId(), m.getDate(), m.getType());
            return m;
        } catch (SQLException e) {
            log.error("Failed to save meal (employeeId={})", m.getEmployeeId(), e);
            throw new DataAccessException("Erro ao salvar refeição", e);
        }
    }

    @Override
    public void update(Meal m) {
        String sql = "UPDATE refeicoes SET colaborador_id = ?, data = ?, horario = ?, preco = ?, tipo = ? " +
                "WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, m.getEmployeeId());
            stmt.setDate(2, Date.valueOf(m.getDate()));
            stmt.setTime(3, Time.valueOf(m.getTime()));
            stmt.setBigDecimal(4, m.getPrice());
            stmt.setString(5, m.getType().name());
            stmt.setInt(6, m.getId());
            stmt.executeUpdate();
            log.info("Meal updated: id={}", m.getId());
        } catch (SQLException e) {
            log.error("Failed to update meal id={}", m.getId(), e);
            throw new DataAccessException("Erro ao atualizar refeição " + m.getId(), e);
        }
    }

    @Override
    public void remove(int id) {
        // Kept for occasional administrative use (not called by the screen) --
        // the normal flow now is deactivate/activate, which preserves the record for the log.
        String sql = "DELETE FROM refeicoes WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            log.warn("Meal permanently removed: id={}", id);
        } catch (SQLException e) {
            log.error("Failed to remove meal id={}", id, e);
            throw new DataAccessException("Erro ao remover refeição " + id, e);
        }
    }

    @Override
    public void deactivate(int id) {
        // Soft delete: preserves the record (never physically deleted) to keep
        // the full history/log, including canceled meals.
        String sql = "UPDATE refeicoes SET ativo = FALSE WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            log.info("Meal deactivated: id={}", id);
        } catch (SQLException e) {
            log.error("Failed to deactivate meal id={}", id, e);
            throw new DataAccessException("Erro ao inativar refeição " + id, e);
        }
    }

    @Override
    public void activate(int id) {
        String sql = "UPDATE refeicoes SET ativo = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            log.info("Meal activated: id={}", id);
        } catch (SQLException e) {
            log.error("Failed to activate meal id={}", id, e);
            throw new DataAccessException("Erro ao ativar refeição " + id, e);
        }
    }

    @Override
    public List<Meal> findByPeriod(LocalDate start, LocalDate end) {
        // Brings back active AND inactive -- this list feeds the screen's
        // table/audit log, so it needs to show everything registered in the period.
        String sql = "SELECT r.*, c.nome AS nome_colaborador, c.empresa_terceirizada AS categoria_colaborador, " +
                "c.uso_compartilhado AS colaborador_compartilhado " +
                "FROM refeicoes r " +
                "JOIN colaboradores c ON c.id = r.colaborador_id " +
                "WHERE r.data BETWEEN ? AND ? ORDER BY r.data, r.horario";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));
            return runQuery(stmt);
        } catch (SQLException e) {
            log.error("Failed to list meals by period {} - {}", start, end, e);
            throw new DataAccessException("Erro ao listar refeições por período", e);
        }
    }

    @Override
    public List<Meal> findActiveByPeriod(LocalDate start, LocalDate end) {
        // Used in the PDF report and in the total spent calculation -- deactivated
        // (canceled) meals must not enter the billing nor the report.
        String sql = "SELECT r.*, c.nome AS nome_colaborador, c.empresa_terceirizada AS categoria_colaborador, " +
                "c.uso_compartilhado AS colaborador_compartilhado " +
                "FROM refeicoes r " +
                "JOIN colaboradores c ON c.id = r.colaborador_id " +
                "WHERE r.ativo = TRUE AND r.data BETWEEN ? AND ? ORDER BY r.data, r.horario";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));
            return runQuery(stmt);
        } catch (SQLException e) {
            log.error("Failed to list active meals by period {} - {}", start, end, e);
            throw new DataAccessException("Erro ao listar refeições ativas por período", e);
        }
    }

    @Override
    public List<Meal> findByEmployeeAndPeriod(int employeeId, LocalDate start, LocalDate end) {
        String sql = "SELECT r.*, c.nome AS nome_colaborador, c.empresa_terceirizada AS categoria_colaborador, " +
                "c.uso_compartilhado AS colaborador_compartilhado " +
                "FROM refeicoes r " +
                "JOIN colaboradores c ON c.id = r.colaborador_id " +
                "WHERE r.colaborador_id = ? AND r.data BETWEEN ? AND ? ORDER BY r.data, r.horario";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(start));
            stmt.setDate(3, Date.valueOf(end));
            return runQuery(stmt);
        } catch (SQLException e) {
            log.error("Failed to list meals for employeeId={}", employeeId, e);
            throw new DataAccessException("Erro ao listar refeições do colaborador", e);
        }
    }

    private List<Meal> runQuery(PreparedStatement stmt) throws SQLException {
        List<Meal> result = new ArrayList<>();
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Meal m = new Meal();
                m.setId(rs.getInt("id"));
                m.setEmployeeId(rs.getInt("colaborador_id"));
                m.setEmployeeName(rs.getString("nome_colaborador"));
                m.setEmployeeCategory(rs.getString("categoria_colaborador"));
                m.setEmployeeSharedUsage(rs.getBoolean("colaborador_compartilhado"));
                m.setDate(rs.getDate("data").toLocalDate());
                m.setTime(rs.getTime("horario").toLocalTime());
                m.setPrice(rs.getBigDecimal("preco"));
                m.setType(MealType.valueOf(rs.getString("tipo")));
                m.setActive(rs.getBoolean("ativo"));
                result.add(m);
            }
        }
        return result;
    }
}
