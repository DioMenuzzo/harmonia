package com.hospital.harmonia.controller;

import com.hospital.harmonia.App;
import com.hospital.harmonia.model.User;
import com.hospital.harmonia.service.UserService;
import com.hospital.harmonia.util.AlertUtil;
import com.hospital.harmonia.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class PreferencesController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(PreferencesController.class);

    @FXML private ImageView profileImagePreview;
    @FXML private Label userInfoLabel;

    private final UserService userService = new UserService();
    private File selectedPhotoFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User user = SessionManager.getLoggedInUser();
        userInfoLabel.setText(user.getUsername() + " - Perfil " + user.getRole());

        if (user.getProfilePicturePath() != null) {
            File photo = new File(user.getProfilePicturePath());
            if (photo.exists()) {
                profileImagePreview.setImage(new Image(photo.toURI().toString()));
            }
        }
    }

    @FXML
    private void onChoosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selecionar foto de perfil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(App.getMainStage());
        if (file != null) {
            selectedPhotoFile = file;
            profileImagePreview.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    private void onSave() {
        User user = SessionManager.getLoggedInUser();
        try {
            if (selectedPhotoFile != null) {
                String savedPath = userService.updateProfilePicture(user.getId(), selectedPhotoFile.toPath());
                user.setProfilePicturePath(savedPath);
            }
            AlertUtil.info("Preferências", "Preferências salvas com sucesso.");
            onBack();
        } catch (IOException e) {
            log.error("Failed to save profile picture for userId={}", user.getId(), e);
            AlertUtil.error("Erro", "Não foi possível salvar a foto de perfil: " + e.getMessage());
        }
    }

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
