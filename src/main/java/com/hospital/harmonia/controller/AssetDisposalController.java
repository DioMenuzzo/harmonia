package com.hospital.harmonia.controller;

import com.hospital.harmonia.App;
import javafx.fxml.FXML;

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

    @FXML
    private void onBack() {
        App.backToHub();
    }
}
