package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.AssetDisposalReportDao;
import com.hospital.harmonia.dao.ExternalAssetDao;
import com.hospital.harmonia.dao.impl.AssetDisposalReportDaoImpl;
import com.hospital.harmonia.dao.impl.ExternalAssetDaoImpl;
import com.hospital.harmonia.model.AssetDisposalReport;
import com.hospital.harmonia.util.ReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AssetDisposalService {

    private static final Logger log = LoggerFactory.getLogger(AssetDisposalService.class);

    private final ExternalAssetDao externalAssetDao = new ExternalAssetDaoImpl();
    private final AssetDisposalReportDao reportDao = new AssetDisposalReportDaoImpl();

    /** Looks up the asset's data in the external database, to pre-fill the report. */
    public Optional<AssetDisposalReport> findAssetData(String assetTagNumber) {
        return externalAssetDao.findAssetByTag(assetTagNumber);
    }

    public AssetDisposalReport issueReport(AssetDisposalReport report) {
        validate(report);
        log.debug("Issuing asset disposal report for assetTag={}", report.getAssetTagNumber());
        return reportDao.save(report);
    }

    public List<AssetDisposalReport> listReports() {
        return reportDao.findAll();
    }

    /** Generates the technical report PDF using JasperReports. */
    public File generateReportPdf(AssetDisposalReport report, File outputFile) throws Exception {
        return ReportGenerator.generatePdf(
                "/reports/laudo_baixa_ativo_report.jrxml",
                java.util.Map.of(),
                Collections.singletonList(report),
                outputFile);
    }

    private void validate(AssetDisposalReport report) {
        if (report.getAssetTagNumber() == null || report.getAssetTagNumber().isBlank()) {
            throw new IllegalArgumentException("Numero de patrimonio e obrigatorio.");
        }
        if (report.getDisposalReason() == null || report.getDisposalReason().isBlank()) {
            throw new IllegalArgumentException("Motivo da baixa e obrigatorio.");
        }
        if (report.getTechnicalOfficer() == null || report.getTechnicalOfficer().isBlank()) {
            throw new IllegalArgumentException("Responsavel tecnico e obrigatorio.");
        }
    }
}
