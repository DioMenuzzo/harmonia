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
    @FXML private VBox technicalSupportCard;
    @FXML private VBox usersCard;
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

        technicalSupportCard.setVisible(user.getRole().canAccessTechnicalSupport());
        technicalSupportCard.setManaged(user.getRole().canAccessTechnicalSupport());

        usersCard.setVisible(user.getRole().canManageUsers());
        usersCard.setManaged(user.getRole().canManageUsers());
    }

    @FXML
    private void onOpenCafeteria() {
        if (!SessionManager.getLoggedInUser().getRole().canAccessCafeteria()) {
            AlertUtil.warning("Acesso negado", "Seu perfil não tem acesso ao módulo Refeitório.");
            return;
        }
        openScreen("/fxml/refeitorio.fxml", "Refeitório - Harmonia");
    }

    @FXML
    private void onOpenAssetDisposal() {
        if (!SessionManager.getLoggedInUser().getRole().canAccessAssetDisposal()) {
            AlertUtil.warning("Acesso negado", "Seu perfil não tem acesso ao módulo Descarte de Ativos.");
            return;
        }
        openScreen("/fxml/descarte_ativos.fxml", "Descarte de Ativos - Harmonia");
    }

    @FXML
    private void onOpenTechnicalSupport() {
        if (!SessionManager.getLoggedInUser().getRole().canAccessTechnicalSupport()) {
            AlertUtil.warning("Acesso negado", "Seu perfil não tem acesso ao módulo Assistência Técnica.");
            return;
        }
        openScreen("/fxml/assistencia_tecnica.fxml", "Assistência Técnica - Harmonia");
    }

    @FXML
    private void onOpenUsers() {
        if (!SessionManager.getLoggedInUser().getRole().canManageUsers()) {
            AlertUtil.warning("Acesso negado", "Seu perfil não tem acesso ao gerenciamento de usuários.");
            return;
        }
        openScreen("/fxml/usuarios.fxml", "Usuários - Harmonia");
    }

    @FXML
    private void onPreferences() {
        openScreen("/fxml/preferencias.fxml", "Preferências - Harmonia");
    }

    @FXML
    private void onAbout() {
        AlertUtil.info("Sobre", "Harmonia - Sistema de Gestão v1.0.0\nMódulos: Refeitório, Descarte de Ativos, Assistência Técnica, Usuários.");
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
            AlertUtil.error("Erro", "Não foi possível voltar para o login: " + e.getMessage());
        }
    }

    private void openScreen(String fxml, String title) {
        try {
            App.switchScene(fxml, title);
        } catch (IOException e) {
            log.error("Failed to open screen {}", fxml, e);
            AlertUtil.error("Erro", "Não foi possível abrir a tela: " + e.getMessage());
        }
    }

    @FXML
    private void onOpenWebsite() {
        BrowserUtil.open("http://ciau.boldrini.org.br/");
    }
}
