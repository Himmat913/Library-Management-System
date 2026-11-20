package com.ucs553.library.ui;

import com.ucs553.library.dao.DataAccessException;
import com.ucs553.library.model.Staff;
import com.ucs553.library.service.AuthService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginView extends StackPane {

    private static final Logger LOG = Logger.getLogger(LoginView.class.getName());

    public LoginView(String appName, AuthService auth, String lastUsername, Consumer<Staff> onSuccess) {
        Label title = new Label(appName);
        title.getStyleClass().add("member-name");
        title.setWrapText(true);
        Label subtitle = new Label("Staff sign in");
        subtitle.getStyleClass().add("muted");

        TextField username = new TextField();
        username.setPromptText("Username");
        username.setText(lastUsername == null ? "" : lastUsername);
        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        Label error = new Label();
        error.setStyle("-fx-text-fill: #b91c1c;");
        error.setWrapText(true);

        Button signIn = new Button("Sign in");
        signIn.getStyleClass().add("primary");
        signIn.setMaxWidth(Double.MAX_VALUE);
        signIn.setDefaultButton(true);

        VBox card = new VBox(12, title, subtitle, username, password, signIn, error);
        try {
            if (auth.defaultAdminStillUsed()) {
                Label hint = new Label("First time here? Sign in as admin / admin123, then change the password.");
                hint.getStyleClass().add("muted");
                hint.setWrapText(true);
                card.getChildren().add(hint);
            }
        } catch (DataAccessException e) {
            LOG.log(Level.WARNING, "Could not check the default account", e);
        }
        card.getStyleClass().add("card");
        card.setMaxWidth(360);
        card.setMaxHeight(VBox.USE_PREF_SIZE);
        card.setPadding(new Insets(24));
        getChildren().add(card);

        signIn.setOnAction(e -> {
            char[] typed = password.getText().toCharArray();
            try {
                Optional<Staff> staff = auth.login(username.getText(), typed);
                if (staff.isPresent()) {
                    onSuccess.accept(staff.get());
                } else {
                    error.setText("Wrong username or password");
                    password.clear();
                }
            } catch (DataAccessException ex) {
                LOG.log(Level.WARNING, "Sign in failed", ex);
                error.setText("Could not check the account, see the log");
            } finally {
                Arrays.fill(typed, ' ');
            }
        });
        Platform.runLater(username.getText().isEmpty() ? username::requestFocus : password::requestFocus);
    }
}
