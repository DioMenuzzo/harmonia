package com.hospital.harmonia;

import com.hospital.harmonia.util.AlertUtil;
import javafx.application.Application;
import javafx.application.Platform;
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
        // Without this, an exception an FXML button/menu handler forgot to
        // catch just prints a stack trace to a console the hospital staff
        // never has open -- from their side the screen simply did nothing
        // when they clicked, with no indication anything went wrong. This
        // turns that into a Portuguese alert instead, and is the standard
        // way to catch what would otherwise be silently swallowed on the
        // JavaFX Application Thread (Thread.currentThread() here IS that
        // thread, since start() runs on it). setDefaultUncaughtExceptionHandler
        // in main() below is the same net for any other thread.
        Thread.currentThread().setUncaughtExceptionHandler(App::handleUncaughtException);

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

    /**
     * Shared by every module controller's "◀ Voltar" button (see onBack() in
     * CafeteriaController, TechnicalSupportController, AgendaTelefonicaController,
     * UserManagementController and PreferencesController). Each controller
     * still needs its own @FXML-annotated onBack() -- FXML's onAction only
     * binds to an instance method on that specific controller -- but they all
     * delegate to this one instead of repeating the same try/switchScene/catch/alert.
     */
    public static void backToHub() {
        try {
            switchScene("/fxml/hub.fxml", "Harmonia");
        } catch (IOException e) {
            log.error("Failed to navigate back to the hub", e);
            AlertUtil.error("Erro", "Não foi possível voltar ao hub: " + e.getMessage());
        }
    }

    /**
     * See the comment in start() -- this is the same safety net for a thread
     * other than the FX Application Thread. Showing a JavaFX Alert is only
     * legal from the FX Application Thread, but this handler can just as
     * well run on some other thread (that's the whole point of also
     * registering it as the JVM-wide default in main() below) -- so the
     * alert itself is always dispatched through Platform.runLater instead of
     * being shown inline, whether or not this call happens to already be on
     * the FX thread.
     */
    private static void handleUncaughtException(Thread thread, Throwable throwable) {
        log.error("Uncaught exception on thread {}", thread.getName(), throwable);
        try {
            Platform.runLater(() -> AlertUtil.error("Erro inesperado",
                    "Ocorreu um erro inesperado: " + throwable.getMessage()
                            + "\n\nSe o problema persistir, informe o suporte técnico."));
        } catch (Exception alertFailure) {
            // Best-effort: if even scheduling the alert fails (e.g. the FX
            // toolkit isn't running anymore during shutdown), don't let that
            // throw again from inside an exception handler -- just log it.
            log.error("Failed to show the uncaught-exception alert", alertFailure);
        }
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(App::handleUncaughtException);
        launch(args);
    }
}
