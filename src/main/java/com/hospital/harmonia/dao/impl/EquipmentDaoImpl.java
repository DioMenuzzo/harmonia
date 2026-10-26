package com.hospital.harmonia.dao.impl;

import com.hospital.harmonia.config.DatabaseConfig;
import com.hospital.harmonia.dao.DataAccessException;
import com.hospital.harmonia.dao.EquipmentDao;
import com.hospital.harmonia.model.Equipment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

// NOTE: table/column names (impressoras_manutencao, tipo_equipamento, modelo,
// marca, patrimonio, numero_serie, setor_origem, assistencia_tecnica,
// data_envio, data_retorno, defeito_relatado, servico_realizado,
// dias_garantia, caminho_anexo_garantia, ativo) are still the ones actually
// in the database -- only the Java-side names were translated. Update these
// SQL strings together with the database migration when that happens.
//
// The table keeps its original name/columns even though the module (and this
// class) now cover any equipment sent for repair, not just printers -- see
// tipo_equipamento, added when the module was generalized to "Assistência
// Técnica". Renaming the table isn't worth the migration risk.
//
// dias_garantia is a newer column (see the MIGRACAO section of schema.sql) --
// it replaces the old em_garantia BOOLEAN, which the app no longer reads or
// writes but which stays in the table (harmless, keeps its default) so
// nothing existing is dropped. caminho_anexo_garantia keeps its original
// column name, but now holds the path of the technical support's note (PDF),
// not only a warranty proof -- see Equipment.notePath.
public class EquipmentDaoImpl implements EquipmentDao {

    private static final Logger log = LoggerFactory.getLogger(EquipmentDaoImpl.class);

    @Override
    public Equipment save(Equipment equipment) {
        String sql = "INSERT INTO impressoras_manutencao " +
                "(tipo_equipamento, modelo, marca, patrimonio, numero_serie, setor_origem, assistencia_tecnica, data_envio, " +
                " data_retorno, defeito_relatado, servico_realizado, dias_garantia, caminho_anexo_garantia) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            fill(stmt, equipment);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    equipment.setId(rs.getInt(1));
                }
            }
            log.info("Equipment shipment registered: id={} assetTag={}", equipment.getId(), equipment.getAssetTag());
            return equipment;
        } catch (SQLException e) {
            log.error("Failed to save equipment record (assetTag={})", equipment.getAssetTag(), e);
            throw new DataAccessException("Erro ao salvar registro de equipamento", e);
        }
    }

    @Override
    public void update(Equipment equipment) {
        String sql = "UPDATE impressoras_manutencao SET tipo_equipamento=?, modelo=?, marca=?, patrimonio=?, numero_serie=?, " +
                "setor_origem=?, assistencia_tecnica=?, data_envio=?, data_retorno=?, defeito_relatado=?, " +
                "servico_realizado=?, dias_garantia=?, caminho_anexo_garantia=? WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            fill(stmt, equipment);
            stmt.setInt(14, equipment.getId());
            stmt.executeUpdate();
            log.info("Equipment record updated: id={}", equipment.getId());
        } catch (SQLException e) {
            log.error("Failed to update equipment record id={}", equipment.getId(), e);
            throw new DataAccessException("Erro ao atualizar registro de equipamento " + equipment.getId(), e);
        }
    }

    private void fill(PreparedStatement stmt, Equipment equipment) throws SQLException {
        stmt.setString(1, equipment.getEquipmentType());
        stmt.setString(2, equipment.getModel());
        stmt.setString(3, equipment.getBrand());
        stmt.setString(4, equipment.getAssetTag());
        stmt.setString(5, equipment.getSerialNumber());
        stmt.setString(6, equipment.getOriginSector());
        stmt.setString(7, equipment.getTechnicalSupport());
        stmt.setDate(8, equipment.getShippedDate() != null ? Date.valueOf(equipment.getShippedDate()) : null);
        stmt.setDate(9, equipment.getReturnedDate() != null ? Date.valueOf(equipment.getReturnedDate()) : null);
        stmt.setString(10, equipment.getReportedIssue());
        stmt.setString(11, equipment.getServiceDone());
        if (equipment.getWarrantyDays() != null) {
            stmt.setInt(12, equipment.getWarrantyDays());
        } else {
            stmt.setNull(12, Types.INTEGER);
        }
        stmt.setString(13, equipment.getNotePath());
    }

    @Override
    public List<Equipment> findAll() {
        return list("SELECT * FROM impressoras_manutencao ORDER BY data_envio DESC");
    }

    @Override
    public Optional<Equipment> findById(int id) {
        String sql = "SELECT * FROM impressoras_manutencao WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("Failed to fetch equipment record id={}", id, e);
            throw new DataAccessException("Erro ao buscar registro de equipamento " + id, e);
        }
    }

    @Override
    public void remove(int id) {
        // Kept for occasional administrative use (not called by the screen) --
        // the normal flow now is deactivate/activate, which preserves the record for the log.
        String sql = "DELETE FROM impressoras_manutencao WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            log.warn("Equipment record permanently removed: id={}", id);
        } catch (SQLException e) {
            log.error("Failed to remove equipment record id={}", id, e);
            throw new DataAccessException("Erro ao excluir registro de equipamento " + id, e);
        }
    }

    @Override
    public void deactivate(int id) {
        // Soft delete: preserves the record (never physically deleted) to keep
        // the full history/log -- same pattern as colaboradores/refeicoes.
        String sql = "UPDATE impressoras_manutencao SET ativo = FALSE WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            log.info("Equipment record deactivated: id={}", id);
        } catch (SQLException e) {
            log.error("Failed to deactivate equipment record id={}", id, e);
            throw new DataAccessException("Erro ao inativar registro de equipamento " + id, e);
        }
    }

    @Override
    public void activate(int id) {
        String sql = "UPDATE impressoras_manutencao SET ativo = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            log.info("Equipment record activated: id={}", id);
        } catch (SQLException e) {
            log.error("Failed to activate equipment record id={}", id, e);
            throw new DataAccessException("Erro ao ativar registro de equipamento " + id, e);
        }
    }

    private List<Equipment> list(String sql) {
        return JdbcListSupport.queryList(log, sql, this::map,
                "Failed to list equipment records", "Erro ao listar equipamentos");
    }

    private Equipment map(ResultSet rs) throws SQLException {
        Equipment equipment = new Equipment();
        equipment.setId(rs.getInt("id"));
        equipment.setEquipmentType(rs.getString("tipo_equipamento"));
        equipment.setModel(rs.getString("modelo"));
        equipment.setBrand(rs.getString("marca"));
        equipment.setAssetTag(rs.getString("patrimonio"));
        equipment.setSerialNumber(rs.getString("numero_serie"));
        equipment.setOriginSector(rs.getString("setor_origem"));
        equipment.setTechnicalSupport(rs.getString("assistencia_tecnica"));
        if (rs.getDate("data_envio") != null) {
            equipment.setShippedDate(rs.getDate("data_envio").toLocalDate());
        }
        if (rs.getDate("data_retorno") != null) {
            equipment.setReturnedDate(rs.getDate("data_retorno").toLocalDate());
        }
        equipment.setReportedIssue(rs.getString("defeito_relatado"));
        equipment.setServiceDone(rs.getString("servico_realizado"));
        int warrantyDays = rs.getInt("dias_garantia");
        equipment.setWarrantyDays(rs.wasNull() ? null : warrantyDays);
        equipment.setNotePath(rs.getString("caminho_anexo_garantia"));
        equipment.setActive(rs.getBoolean("ativo"));
        return equipment;
    }
}
