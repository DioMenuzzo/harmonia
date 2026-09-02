package com.hospital.harmonia.controller;

import com.hospital.harmonia.App;
import com.hospital.harmonia.util.AlertUtil;
import javafx.fxml.FXML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Placeholder screen for the Printers module.
 *
 * The full functionality (registering a printer sent for repair, attaching a
 * warranty proof, registering its return, table of printers under
 * maintenance) already exists implemented in PrinterService/PrinterDaoImpl and
 * will be reactivated here in a future update. For now this screen just shows
 * an "under construction" notice and lets the user go back to the hub, so the
 * module doesn't break navigation nor prevent the rest of the system from compiling.
 */
public class PrintersController {

    private static final Logger log = LoggerFactory.getLogger(PrintersController.class);

    @FXML
    private void onBack() {
        try {
            App.switchScene("/fxml/hub.fxml", "Harmonia");
        } catch (IOException e) {
            log.error("Failed to navigate back to the hub", e);
            AlertUtil.error("Erro", "Nao foi possivel voltar ao hub: " + e.getMessage());
        }
    }
}
