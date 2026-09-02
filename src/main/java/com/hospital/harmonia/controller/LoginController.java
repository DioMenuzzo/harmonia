package com.hospital.harmonia.controller;

import com.hospital.harmonia.App;
import com.hospital.harmonia.model.User;
import com.hospital.harmonia.service.AuthService;
import com.hospital.harmonia.util.AlertUtil;
import com.hospital.harmonia.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();

    // Store where the mouse clicked inside the window, to calculate the
    // offset while dragging -- replaces the "drag by the title bar" that the
    // undecorated window no longer has.
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    private void onDragPressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    private void onDragMoved(MouseEvent event) {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    @FXML
    private void onMinimize() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void onClose() {
        // Closes the whole application (the login window is the only one open at this point).
        Platform.exit();
    }

    @FXML
    private void onLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password == null || password.isEmpty()) {
            showError("Informe usuario e senha.");
            return;
        }

        Optional<User> result;
        try {
            result = authService.authenticate(username, password);
        } catch (Exception e) {
            // Database down, wrong credentials in db.properties, schema not
            // created yet, etc. Previously this wasn't handled here: the error
            // wouldn't show on screen, it would just vanish (or fail silently).
            log.error("Failed to authenticate username={}", username, e);
            showError("Nao foi possivel conectar ao banco de dados. Tente novamente em instantes.");
            AlertUtil.error("Erro de conexao", "Nao foi possivel autenticar: " + e.getMessage());
            return;
        }

        if (result.isEmpty()) {
            showError("Usuario ou senha invalidos.");
            passwordField.clear();
            return;
        }

        SessionManager.login(result.get());

        try {
            // Closes the login window (undecorated) and opens the main window
            // (normally decorated) already showing the hub.
            Stage loginStage = (Stage) usernameField.getScene().getWindow();
            loginStage.close();
            App.openHubInMainWindow();
        } catch (IOException e) {
            log.error("Failed to open the hub after login", e);
            AlertUtil.error("Erro", "Nao foi possivel abrir a tela inicial: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
