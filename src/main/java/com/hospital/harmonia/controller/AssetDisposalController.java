package com.hospital.harmonia.controller;

import com.hospital.harmonia.App;
import com.hospital.harmonia.util.AlertUtil;
import javafx.fxml.FXML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Placeholder screen for the Asset Disposal module.
 *
 * The full functionality (issuing a disposal report, querying the external
 * asset system, table of issued reports, PDF generation) already exists
 * implemented in AssetDisposalService/ExternalAssetDaoImpl/AssetDisposalReportDaoImpl
 * and will be reactivated here in a future update. For now this screen just
 * shows an "under construction" notice and lets the user go back to the hub,
 * so the module doesn't break navigation nor prevent the rest of the system from compiling.
 */
public class AssetDisposalController {

    private static final Logger log = LoggerFactory.getLogger(AssetDisposalController.class);

    @FXML
    private void onBack() {
        try {
            App.switchScene("/fxml/hub.fxml", "Harmonia");
        } catch (IOException e) {
            log.error("Failed to navigate back to the hub", e);
            AlertUtil.error("Erro", "Não foi possível voltar ao hub: " + e.getMessage());
        }
    }
}
