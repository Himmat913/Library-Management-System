package com.ucs553.library.ui;

import com.ucs553.library.model.Role;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

class StaffDialog extends Dialog<StaffDialog.Form> {

    record Form(String name, String email, String username, Role role, char[] password) {
    }

    private final TextField name = new TextField();
    private final TextField email = new TextField();
    private final TextField username = new TextField();
    private final PasswordField password = new PasswordField();
    private final ComboBox<Role> role = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
    private final Label error = new Label();
    private Form result;

    StaffDialog(Window owner) {
        initOwner(owner);
        setTitle("Add staff account");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        role.setValue(Role.LIBRARIAN);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Name"), 0, 0);
        grid.add(name, 1, 0);
        grid.add(new Label("Email"), 0, 1);
        grid.add(email, 1, 1);
        grid.add(new Label("Username"), 0, 2);
        grid.add(username, 1, 2);
        grid.add(new Label("Password"), 0, 3);
        grid.add(password, 1, 3);
        grid.add(new Label("Role"), 0, 4);
        grid.add(role, 1, 4);

        error.setStyle("-fx-text-fill: #b91c1c;");
        error.setWrapText(true);
        VBox content = new VBox(12, grid, error);
        content.setPadding(new Insets(16));
        getDialogPane().setContent(content);

        getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            if (name.getText().isBlank() || email.getText().isBlank() || username.getText().isBlank()) {
                error.setText("Name, email and username are required");
                event.consume();
                return;
            }
            result = new Form(name.getText(), email.getText(), username.getText(), role.getValue(),
                    password.getText().toCharArray());
        });
        setResultConverter(button -> button == ButtonType.OK ? result : null);
    }
}
