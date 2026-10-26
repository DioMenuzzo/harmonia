package com.hospital.harmonia.service;

import com.hospital.harmonia.dao.AssetDisposalReportDao;
import com.hospital.harmonia.dao.ExternalAssetDao;
import com.hospital.harmonia.model.AssetDisposalReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises AssetDisposalService's validation rules and its delegation to
 * the external (asset lookup) and local (report storage) DAOs, with
 * in-memory fakes instead of real databases -- see FakeExternalAssetDao and
 * FakeAssetDisposalReportDao below.
 *
 * generateReportPdf() is deliberately not covered here: it renders a real
 * JasperReports template to a PDF file, which needs the actual report
 * resource and isn't something a plain unit test should be driving.
 */
class AssetDisposalServiceTest {

    private FakeExternalAssetDao fakeExternalDao;
    private FakeAssetDisposalReportDao fakeReportDao;
    private AssetDisposalService service;

    @BeforeEach
    void setUp() {
        fakeExternalDao = new FakeExternalAssetDao();
        fakeReportDao = new FakeAssetDisposalReportDao();
        service = new AssetDisposalService(fakeExternalDao, fakeReportDao);
    }

    private static AssetDisposalReport valid() {
        AssetDisposalReport report = new AssetDisposalReport();
        report.setAssetTagNumber("PAT-00123");
        report.setDisposalReason("Equipamento obsoleto, sem reposição de peças.");
        report.setTechnicalOfficer("João da Silva");
        return report;
    }

    @Test
    void issueReport_rejectsMissingAssetTagNumber() {
        AssetDisposalReport report = valid();
        report.setAssetTagNumber(" ");
        assertThrows(IllegalArgumentException.class, () -> service.issueReport(report));
        assertTrue(fakeReportDao.saved.isEmpty());
    }

    @Test
    void issueReport_rejectsMissingDisposalReason() {
        AssetDisposalReport report = valid();
        report.setDisposalReason(null);
        assertThrows(IllegalArgumentException.class, () -> service.issueReport(report));
    }

    @Test
    void issueReport_rejectsMissingTechnicalOfficer() {
        AssetDisposalReport report = valid();
        report.setTechnicalOfficer("");
        assertThrows(IllegalArgumentException.class, () -> service.issueReport(report));
    }

    @Test
    void issueReport_savesAValidReport() {
        AssetDisposalReport saved = service.issueReport(valid());
        assertEquals(1, saved.getId().intValue());
        assertEquals(1, fakeReportDao.saved.size());
    }

    @Test
    void findAssetData_returnsWhatTheExternalDaoHasForThatTag() {
        AssetDisposalReport fromExternalSystem = new AssetDisposalReport();
        fromExternalSystem.setAssetTagNumber("PAT-00123");
        fromExternalSystem.setAssetDescription("Notebook Dell Latitude");
        fakeExternalDao.byTag.put("PAT-00123", fromExternalSystem);

        Optional<AssetDisposalReport> result = service.findAssetData("PAT-00123");

        assertTrue(result.isPresent());
        assertEquals("Notebook Dell Latitude", result.get().getAssetDescription());
    }

    @Test
    void findAssetData_isEmptyForATagTheExternalSystemDoesNotKnow() {
        assertFalse(service.findAssetData("PAT-DOES-NOT-EXIST").isPresent());
    }

    @Test
    void findAll_delegatesToTheReportDao() {
        service.issueReport(valid());
        AssetDisposalReport second = valid();
        second.setAssetTagNumber("PAT-00456");
        service.issueReport(second);

        assertEquals(2, service.findAll().size());
    }

    /** Minimal in-memory stand-in for ExternalAssetDaoImpl -- no external database involved. */
    private static class FakeExternalAssetDao implements ExternalAssetDao {
        final Map<String, AssetDisposalReport> byTag = new HashMap<>();

        @Override
        public Optional<AssetDisposalReport> findAssetByTag(String assetTagNumber) {
            return Optional.ofNullable(byTag.get(assetTagNumber));
        }
    }

    /** Minimal in-memory stand-in for AssetDisposalReportDaoImpl -- no database involved. */
    private static class FakeAssetDisposalReportDao implements AssetDisposalReportDao {
        final List<AssetDisposalReport> saved = new ArrayList<>();
        private int nextId = 1;

        @Override
        public AssetDisposalReport save(AssetDisposalReport report) {
            report.setId(nextId++);
            saved.add(report);
            return report;
        }

        @Override
        public List<AssetDisposalReport> findAll() {
            return List.copyOf(saved);
        }
    }
}
