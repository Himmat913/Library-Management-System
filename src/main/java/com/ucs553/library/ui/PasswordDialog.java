package com.ucs553.library.ui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

class PasswordDialog extends Dialog<PasswordDialog.Passwords> {

    record Passwords(char[] current, char[] replacement) {
    }

    private final PasswordField current = new PasswordField();
    private final PasswordField replacement = new PasswordField();
    private final PasswordField confirm = new PasswordField();
    private final Label error = new Label();
    private Passwords result;

    PasswordDialog(Window owner) {
        initOwner(owner);
        setTitle("Change password");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Current password"), 0, 0);
        grid.add(current, 1, 0);
        grid.add(new Label("New password"), 0, 1);
        grid.add(replacement, 1, 1);
        grid.add(new Label("Repeat new password"), 0, 2);
        grid.add(confirm, 1, 2);

        error.setStyle("-fx-text-fill: #b91c1c;");
        error.setWrapText(true);
        VBox content = new VBox(12, grid, error);
        content.setPadding(new Insets(16));
        getDialogPane().setContent(content);

        getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            if (!replacement.getText().equals(confirm.getText())) {
                error.setText("The new passwords do not match");
                event.consume();
                return;
            }
            result = new Passwords(current.getText().toCharArray(), replacement.getText().toCharArray());
        });
        setResultConverter(button -> button == ButtonType.OK ? result : null);
    }
}
