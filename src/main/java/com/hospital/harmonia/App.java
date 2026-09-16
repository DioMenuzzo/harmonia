package com.hospital.harmonia;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * Main class of the JavaFX application.
 *
 * The LOGIN window opens in its own Stage, without the operating system's
 * decoration (no native title bar/minimize/close) -- that's why it's
 * independent from the main window. This is necessary because a Stage's
 * style (decorated or not) can only be set BEFORE its first show() and can't
 * be changed afterwards; since the hub and the modules keep the normal
 * Windows decoration, they use the main Stage (mainStage), which only appears
 * after login completes successfully (see openHubInMainWindow(), called by
 * LoginController).
 */
public class App extends Application {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private static Stage mainStage;

    @Override
    public void start(Stage stage) throws IOException {
        log.info("Harmonia starting up...");
        mainStage = stage;
        mainStage.setTitle("Harmonia");
        mainStage.getIcons().add(new javafx.scene.image.Image(
                Objects.requireNonNull(App.class.getResourceAsStream("/images/app-icon.png"))));

        openLoginWindow();
    }

    @Override
    public void stop() {
        log.info("Harmonia shutting down.");
    }

    /** Opens the login screen in its own window, without a title bar (StageStyle.UNDECORATED). */
    public static void openLoginWindow() throws IOException {
        Stage loginStage = new Stage();
        loginStage.initStyle(StageStyle.UNDECORATED);
        loginStage.getIcons().add(new javafx.scene.image.Image(
                Objects.requireNonNull(App.class.getResourceAsStream("/images/app-icon.png"))));

        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/css/styles.css")).toExternalForm());

        loginStage.setScene(scene);
        loginStage.centerOnScreen();
        loginStage.show();
    }

    /** Called by LoginController after a successful login: shows the (decorated) main window with the hub. */
    public static void openHubInMainWindow() throws IOException {
        switchScene("/fxml/hub.fxml", "Harmonia");
        mainStage.show();
    }

    /**
     * Switches the scene shown in the MAIN window, loading the given FXML.
     * Used by the controllers to navigate between the hub and the modules.
     */
    public static void switchScene(String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlPath));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/css/styles.css")).toExternalForm());
        mainStage.setScene(scene);
        mainStage.setTitle(title);
        // Each screen's FXML declares its own prefWidth/prefHeight (the hub is
        // deliberately small -- 900x450 -- while a module like Refeitório or
        // Assistência Técnica is much bigger, e.g. 1150x680). setScene() alone
        // does NOT resize an already-shown Stage to match the new scene's
        // preferred size -- without this call the window stays stuck at
        // whatever size it had on the previous screen (typically the hub's),
        // so a bigger screen shows up cramped/clipped instead of at its
        // designed size. Skipped while maximized: sizeToScene() would fight
        // the OS-maximized size and isn't needed there anyway (the window
        // already fills the screen).
        if (!mainStage.isMaximized()) {
            mainStage.sizeToScene();
        }
        mainStage.centerOnScreen();
    }

    public static Stage getMainStage() {
        return mainStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
