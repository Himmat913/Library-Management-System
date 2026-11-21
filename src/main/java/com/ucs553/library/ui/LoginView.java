package com.ucs553.library.ui;

import com.ucs553.library.dao.DataAccessException;
import com.ucs553.library.model.Staff;
import com.ucs553.library.service.AuthService;
import com.ucs553.library.service.MemberLogin;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginView extends StackPane {

    private static final Logger LOG = Logger.getLogger(LoginView.class.getName());

    private final AuthService auth;
    private final Consumer<Staff> onStaff;
    private final Consumer<MemberLogin> onMember;
    private final TextField username = new TextField();
    private final PasswordField password = new PasswordField();
    private final Label error = new Label();
    private final Label hint = new Label();
    private final String lastUsername;
    private boolean memberMode;

    public LoginView(String appName, AuthService auth, String lastUsername,
                     Consumer<Staff> onStaff, Consumer<MemberLogin> onMember) {
        this.auth = auth;
        this.onStaff = onStaff;
        this.onMember = onMember;
        this.lastUsername = lastUsername == null ? "" : lastUsername;
        getStyleClass().add("login-root");

        Label title = new Label(appName);
        title.getStyleClass().add("member-name");
        title.setWrapText(true);
        Label subtitle = new Label("Sign in to continue");
        subtitle.getStyleClass().add("muted");

        ToggleGroup group = new ToggleGroup();
        ToggleButton staffTab = new ToggleButton("Staff");
        ToggleButton memberTab = new ToggleButton("Member");
        for (ToggleButton tab : new ToggleButton[]{staffTab, memberTab}) {
            tab.setToggleGroup(group);
            tab.getStyleClass().add("segment");
            tab.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(tab, javafx.scene.layout.Priority.ALWAYS);
        }
        group.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) {
                old.setSelected(true);
            }
        });
        staffTab.selectedProperty().addListener((obs, was, now) -> {
            if (now) {
                switchMode(false);
            }
        });
        memberTab.selectedProperty().addListener((obs, was, now) -> {
            if (now) {
                switchMode(true);
            }
        });
        HBox tabs = new HBox(staffTab, memberTab);

        password.setPromptText("Password");
        error.setStyle("-fx-text-fill: #b91c1c;");
        error.setWrapText(true);
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);

        Button signIn = new Button("Sign in");
        signIn.getStyleClass().add("primary");
        signIn.setMaxWidth(Double.MAX_VALUE);
        signIn.setDefaultButton(true);
        signIn.setOnAction(e -> signIn());

        VBox card = new VBox(12, title, subtitle, tabs, username, password, signIn, error, hint);
        card.getStyleClass().addAll("card", "login-card");
        card.setMaxWidth(360);
        card.setMaxHeight(VBox.USE_PREF_SIZE);
        card.setPadding(new Insets(24));
        getChildren().add(card);

        staffTab.setSelected(true);
        switchMode(false);
    }

    private void switchMode(boolean member) {
        memberMode = member;
        error.setText("");
        password.clear();
        if (member) {
            username.setPromptText("Email address");
            username.clear();
            hint.setText("Members sign in with the email they registered with. Ask the library desk if you do not have a password yet.");
        } else {
            username.setPromptText("Username");
            username.setText(lastUsername);
            hint.setText("");
            try {
                if (auth.defaultAdminStillUsed()) {
                    hint.setText("First time here? Sign in as admin / admin123, then change the password.");
                }
            } catch (DataAccessException e) {
                LOG.log(Level.WARNING, "Could not check the default account", e);
            }
        }
        Platform.runLater(username.getText().isEmpty() ? username::requestFocus : password::requestFocus);
    }

    private void signIn() {
        char[] typed = password.getText().toCharArray();
        try {
            if (memberMode) {
                Optional<MemberLogin> login = auth.loginMember(username.getText(), typed);
                if (login.isPresent()) {
                    onMember.accept(login.get());
                    return;
                }
                error.setText("Wrong email or password");
            } else {
                Optional<Staff> staff = auth.login(username.getText(), typed);
                if (staff.isPresent()) {
                    onStaff.accept(staff.get());
                    return;
                }
                error.setText("Wrong username or password");
            }
            password.clear();
        } catch (DataAccessException ex) {
            LOG.log(Level.WARNING, "Sign in failed", ex);
            error.setText("Could not check the account, see the log");
        } finally {
            Arrays.fill(typed, ' ');
        }
    }
}
