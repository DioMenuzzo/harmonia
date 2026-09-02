package com.hospital.harmonia.dao.impl;

import com.hospital.harmonia.config.DatabaseConfig;
import com.hospital.harmonia.dao.DataAccessException;
import com.hospital.harmonia.dao.PrinterDao;
import com.hospital.harmonia.model.Printer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// NOTE: table/column names (impressoras_manutencao, modelo, patrimonio,
// setor_origem, assistencia_tecnica, data_envio, data_retorno,
// defeito_relatado, servico_realizado, em_garantia, caminho_anexo_garantia)
// are still the ones actually in the database -- only the Java-side names
// were translated. Update these SQL strings together with the database
// migration when that happens.
public class PrinterDaoImpl implements PrinterDao {

    private static final Logger log = LoggerFactory.getLogger(PrinterDaoImpl.class);

    @Override
    public Printer save(Printer printer) {
        String sql = "INSERT INTO impressoras_manutencao " +
                "(modelo, patrimonio, setor_origem, assistencia_tecnica, data_envio, data_retorno, " +
                " defeito_relatado, servico_realizado, em_garantia, caminho_anexo_garantia) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            fill(stmt, printer);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    printer.setId(rs.getInt(1));
                }
            }
            log.info("Printer shipment registered: id={} assetTag={}", printer.getId(), printer.getAssetTag());
            return printer;
        } catch (SQLException e) {
            log.error("Failed to save printer record (assetTag={})", printer.getAssetTag(), e);
            throw new DataAccessException("Erro ao salvar registro de impressora", e);
        }
    }

    @Override
    public void update(Printer printer) {
        String sql = "UPDATE impressoras_manutencao SET modelo=?, patrimonio=?, setor_origem=?, " +
                "assistencia_tecnica=?, data_envio=?, data_retorno=?, defeito_relatado=?, " +
                "servico_realizado=?, em_garantia=?, caminho_anexo_garantia=? WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            fill(stmt, printer);
            stmt.setInt(11, printer.getId());
            stmt.executeUpdate();
            log.info("Printer record updated: id={}", printer.getId());
        } catch (SQLException e) {
            log.error("Failed to update printer record id={}", printer.getId(), e);
            throw new DataAccessException("Erro ao atualizar registro de impressora " + printer.getId(), e);
        }
    }

    private void fill(PreparedStatement stmt, Printer printer) throws SQLException {
        stmt.setString(1, printer.getModel());
        stmt.setString(2, printer.getAssetTag());
        stmt.setString(3, printer.getOriginSector());
        stmt.setString(4, printer.getTechnicalSupport());
        stmt.setDate(5, printer.getShippedDate() != null ? Date.valueOf(printer.getShippedDate()) : null);
        stmt.setDate(6, printer.getReturnedDate() != null ? Date.valueOf(printer.getReturnedDate()) : null);
        stmt.setString(7, printer.getReportedIssue());
        stmt.setString(8, printer.getServiceDone());
        stmt.setBoolean(9, printer.isUnderWarranty());
        stmt.setString(10, printer.getWarrantyAttachmentPath());
    }

    @Override
    public List<Printer> findAll() {
        String sql = "SELECT * FROM impressoras_manutencao ORDER BY data_envio DESC";
        List<Printer> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Printer printer = new Printer();
                printer.setId(rs.getInt("id"));
                printer.setModel(rs.getString("modelo"));
                printer.setAssetTag(rs.getString("patrimonio"));
                printer.setOriginSector(rs.getString("setor_origem"));
                printer.setTechnicalSupport(rs.getString("assistencia_tecnica"));
                if (rs.getDate("data_envio") != null) printer.setShippedDate(rs.getDate("data_envio").toLocalDate());
                if (rs.getDate("data_retorno") != null) printer.setReturnedDate(rs.getDate("data_retorno").toLocalDate());
                printer.setReportedIssue(rs.getString("defeito_relatado"));
                printer.setServiceDone(rs.getString("servico_realizado"));
                printer.setUnderWarranty(rs.getBoolean("em_garantia"));
                printer.setWarrantyAttachmentPath(rs.getString("caminho_anexo_garantia"));
                list.add(printer);
            }
            return list;
        } catch (SQLException e) {
            log.error("Failed to list printers", e);
            throw new DataAccessException("Erro ao listar impressoras", e);
        }
    }
}
