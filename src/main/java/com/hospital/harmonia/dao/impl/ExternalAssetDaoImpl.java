package com.hospital.harmonia.dao.impl;

import com.hospital.harmonia.config.DatabaseConfig;
import com.hospital.harmonia.dao.DataAccessException;
import com.hospital.harmonia.dao.ExternalAssetDao;
import com.hospital.harmonia.model.AssetDisposalReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class ExternalAssetDaoImpl implements ExternalAssetDao {

    private static final Logger log = LoggerFactory.getLogger(ExternalAssetDaoImpl.class);

    // EXAMPLE query -- adapt to the real schema of the hospital's asset system.
    private static final String SQL_FIND_ASSET =
            "SELECT numero_patrimonio, descricao, localizacao, data_aquisicao, valor_aquisicao " +
            "FROM ativos_patrimoniais WHERE numero_patrimonio = ?";

    @Override
    public Optional<AssetDisposalReport> findAssetByTag(String assetTagNumber) {
        try (Connection conn = DatabaseConfig.getExternalConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ASSET)) {

            stmt.setString(1, assetTagNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                AssetDisposalReport report = new AssetDisposalReport();
                report.setAssetTagNumber(rs.getString("numero_patrimonio"));
                report.setAssetDescription(rs.getString("descricao"));
                report.setLocation(rs.getString("localizacao"));
                if (rs.getDate("data_aquisicao") != null) {
                    report.setAcquisitionDate(rs.getDate("data_aquisicao").toLocalDate());
                }
                report.setAcquisitionValue(rs.getString("valor_aquisicao"));
                return Optional.of(report);
            }

        } catch (SQLException e) {
            log.error("Failed to query external asset database for assetTag={}", assetTagNumber, e);
            throw new DataAccessException(
                "Erro ao consultar o banco de dados externo de patrimonio. " +
                "Verifique a configuracao 'external.db.*' em db.properties e a disponibilidade da rede.", e);
        }
    }
}
