package com.ucs553.library.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

final class Dialogs {

    private Dialogs() {
    }

    static void error(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.initOwner(owner);
        alert.showAndWait();
    }

    static void info(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.initOwner(owner);
        alert.showAndWait();
    }

    static boolean confirm(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.initOwner(owner);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}
