package com.hospital.harmonia.controller;

import com.hospital.harmonia.App;
import com.hospital.harmonia.model.User;
import com.hospital.harmonia.util.AlertUtil;
import com.hospital.harmonia.util.BrowserUtil;
import com.hospital.harmonia.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class HubController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(HubController.class);

    @FXML private VBox cafeteriaCard;
    @FXML private VBox assetDisposalCard;
    @FXML private VBox printersCard;
    @FXML private Label userNameLabel;
    @FXML private ImageView profileImageView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User user = SessionManager.getLoggedInUser();
        if (user == null) {
            return; // safety guard; shouldn't happen in this flow
        }

        userNameLabel.setText(user.getDisplayName() + " (" + user.getRole() + ")");

        if (user.getProfilePicturePath() != null) {
            File photo = new File(user.getProfilePicturePath());
            if (photo.exists()) {
                profileImageView.setImage(new Image(photo.toURI().toString()));
            }
        }

        // Role-based access control: modules the user doesn't have permission
        // for are hidden in the hub. The same check is repeated in the
        // onOpen* methods below (defense in depth).
        cafeteriaCard.setVisible(user.getRole().canAccessCafeteria());
        cafeteriaCard.setManaged(user.getRole().canAccessCafeteria());

        assetDisposalCard.setVisible(user.getRole().canAccessAssetDisposal());
        assetDisposalCard.setManaged(user.getRole().canAccessAssetDisposal());

        printersCard.setVisible(user.getRole().canAccessPrinters());
        printersCard.setManaged(user.getRole().canAccessPrinters());
    }

    @FXML
    private void onOpenCafeteria() {
        if (!SessionManager.getLoggedInUser().getRole().canAccessCafeteria()) {
            AlertUtil.warning("Acesso negado", "Seu perfil nao tem acesso ao modulo Refeitorio.");
            return;
        }
        openScreen("/fxml/refeitorio.fxml", "Refeitorio - Harmonia");
    }

    @FXML
    private void onOpenAssetDisposal() {
        if (!SessionManager.getLoggedInUser().getRole().canAccessAssetDisposal()) {
            AlertUtil.warning("Acesso negado", "Seu perfil nao tem acesso ao modulo Descarte de Ativos.");
            return;
        }
        openScreen("/fxml/descarte_ativos.fxml", "Descarte de Ativos - Harmonia");
    }

    @FXML
    private void onOpenPrinters() {
        if (!SessionManager.getLoggedInUser().getRole().canAccessPrinters()) {
            AlertUtil.warning("Acesso negado", "Seu perfil nao tem acesso ao modulo Impressoras.");
            return;
        }
        openScreen("/fxml/impressoras.fxml", "Impressoras - Harmonia");
    }

    @FXML
    private void onPreferences() {
        openScreen("/fxml/preferencias.fxml", "Preferencias - Harmonia");
    }

    @FXML
    private void onAbout() {
        AlertUtil.info("Sobre", "Harmonia - Sistema de Gestão v1.0.0\nModulos: Refeitório, Descarte de Ativos, Impressoras.");
    }

    @FXML
    private void onLogout() {
        SessionManager.logout();
        try {
            // The (decorated) main window is hidden, and the login screen
            // opens again in its own undecorated window.
            App.getMainStage().hide();
            App.openLoginWindow();
        } catch (IOException e) {
            log.error("Failed to return to the login screen after logout", e);
            AlertUtil.error("Erro", "Nao foi possivel voltar para o login: " + e.getMessage());
        }
    }

    private void openScreen(String fxml, String title) {
        try {
            App.switchScene(fxml, title);
        } catch (IOException e) {
            log.error("Failed to open screen {}", fxml, e);
            AlertUtil.error("Erro", "Nao foi possivel abrir a tela: " + e.getMessage());
        }
    }

    @FXML
    private void onOpenWebsite() {
        BrowserUtil.open("http://ciau.boldrini.org.br/");
    }
}
