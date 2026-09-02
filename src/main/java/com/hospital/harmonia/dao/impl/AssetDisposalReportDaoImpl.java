package com.hospital.harmonia.dao.impl;

import com.hospital.harmonia.config.DatabaseConfig;
import com.hospital.harmonia.dao.AssetDisposalReportDao;
import com.hospital.harmonia.dao.DataAccessException;
import com.hospital.harmonia.model.AssetDisposalReport;
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

public class AssetDisposalReportDaoImpl implements AssetDisposalReportDao {

    private static final Logger log = LoggerFactory.getLogger(AssetDisposalReportDaoImpl.class);

    @Override
    public AssetDisposalReport save(AssetDisposalReport report) {
        String sql = "INSERT INTO laudos_baixa_ativos " +
                "(numero_patrimonio, descricao_ativo, localizacao, data_aquisicao, valor_aquisicao, " +
                " motivo_baixa, parecer_tecnico, responsavel_tecnico, data_laudo, situacao) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, report.getAssetTagNumber());
            stmt.setString(2, report.getAssetDescription());
            stmt.setString(3, report.getLocation());
            stmt.setDate(4, report.getAcquisitionDate() != null ? Date.valueOf(report.getAcquisitionDate()) : null);
            stmt.setString(5, report.getAcquisitionValue());
            stmt.setString(6, report.getDisposalReason());
            stmt.setString(7, report.getTechnicalOpinion());
            stmt.setString(8, report.getTechnicalOfficer());
            stmt.setDate(9, Date.valueOf(report.getReportDate()));
            stmt.setString(10, report.getStatus());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    report.setId(rs.getInt(1));
                }
            }
            log.info("Asset disposal report issued: id={} assetTag={}", report.getId(), report.getAssetTagNumber());
            return report;
        } catch (SQLException e) {
            log.error("Failed to save asset disposal report (assetTag={})", report.getAssetTagNumber(), e);
            throw new DataAccessException("Erro ao salvar laudo de baixa de ativo", e);
        }
    }

    @Override
    public List<AssetDisposalReport> findAll() {
        String sql = "SELECT * FROM laudos_baixa_ativos ORDER BY data_laudo DESC";
        List<AssetDisposalReport> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                AssetDisposalReport report = new AssetDisposalReport();
                report.setId(rs.getInt("id"));
                report.setAssetTagNumber(rs.getString("numero_patrimonio"));
                report.setAssetDescription(rs.getString("descricao_ativo"));
                report.setLocation(rs.getString("localizacao"));
                if (rs.getDate("data_aquisicao") != null) {
                    report.setAcquisitionDate(rs.getDate("data_aquisicao").toLocalDate());
                }
                report.setAcquisitionValue(rs.getString("valor_aquisicao"));
                report.setDisposalReason(rs.getString("motivo_baixa"));
                report.setTechnicalOpinion(rs.getString("parecer_tecnico"));
                report.setTechnicalOfficer(rs.getString("responsavel_tecnico"));
                report.setReportDate(rs.getDate("data_laudo").toLocalDate());
                report.setStatus(rs.getString("situacao"));
                list.add(report);
            }
            return list;
        } catch (SQLException e) {
            log.error("Failed to list asset disposal reports", e);
            throw new DataAccessException("Erro ao listar laudos de baixa de ativos", e);
        }
    }
}
