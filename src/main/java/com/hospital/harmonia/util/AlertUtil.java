package com.hospital.harmonia.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public final class AlertUtil {

    // JavaFX only lets the window's own "X" button actually close an Alert
    // when at least one of its buttons carries ButtonData.CANCEL_CLOSE --
    // without one, clicking "X" is silently ignored (a known JavaFX quirk,
    // already worked around the same way in CafeteriaController's
    // showSkippedItemsDialog). ButtonType.OK carries OK_DONE instead, so
    // error/info/warning below -- built through show(), used all over the
    // app -- all had the "X doesn't close the alert" bug. confirm() is
    // unaffected: ButtonType.CANCEL already carries CANCEL_CLOSE.
    private static final ButtonType CLOSEABLE_OK = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);

    private AlertUtil() {
    }

    public static void error(String title, String message) {
        show(Alert.AlertType.ERROR, title, message);
    }

    public static void info(String title, String message) {
        show(Alert.AlertType.INFORMATION, title, message);
    }

    public static void warning(String title, String message) {
        show(Alert.AlertType.WARNING, title, message);
    }

    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, CLOSEABLE_OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
