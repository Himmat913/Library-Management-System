package com.ucs553.library.ui;

import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.Member;
import com.ucs553.library.service.AuthService;
import com.ucs553.library.service.MemberPortal;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

// the window a signed-in member sees: the catalogue, their own account and their notifications
public class MemberMainView extends BorderPane {

    private static final Logger LOG = Logger.getLogger(MemberMainView.class.getName());

    private final ToggleGroup group = new ToggleGroup();
    private final VBox sidebar = new VBox(4);
    private final StringProperty status = new SimpleStringProperty("");
    private final MemberPortal portal;
    private final AuthService auth;
    private ToggleButton noticesButton;

    public MemberMainView(String appName, MemberPortal portal, AuthService auth, Runnable onSignOut) {
        this.portal = portal;
        this.auth = auth;
        Member member = portal.member();

        Label title = new Label(appName);
        title.getStyleClass().add("app-title");
        title.setWrapText(true);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(16, 10, 16, 10));
        sidebar.setPrefWidth(190);
        sidebar.getChildren().add(title);
        setLeft(sidebar);

        Label statusLabel = new Label();
        statusLabel.textProperty().bind(status);
        HBox statusBar = new HBox(statusLabel);
        statusBar.getStyleClass().add("status-bar");
        setBottom(statusBar);

        group.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) {
                old.setSelected(true);
            }
        });

        addPage("Catalogue", new MemberCatalogueView(portal, status));
        addPage("My account", new MemberAccountView(portal, status));
        noticesButton = addPage("Notifications", new MemberNoticesView(portal, status, this::updateBadge));
        updateBadge();

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label who = new Label(member.getName() + "\nMember");
        who.getStyleClass().add("user-label");
        Button password = new Button("Change password");
        password.setMaxWidth(Double.MAX_VALUE);
        password.setOnAction(e -> changePassword());
        Button signOut = new Button("Sign out");
        signOut.setMaxWidth(Double.MAX_VALUE);
        signOut.setOnAction(e -> onSignOut.run());
        sidebar.getChildren().addAll(spacer, who, password, signOut);
    }

    private <V extends Region & Refreshable> ToggleButton addPage(String name, V view) {
        ToggleButton button = new ToggleButton(name);
        button.setToggleGroup(group);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("nav-button");
        button.selectedProperty().addListener((obs, was, selected) -> {
            if (selected) {
                view.refresh();
                setCenter(view);
                updateBadge();
            }
        });
        sidebar.getChildren().add(button);
        if (group.getSelectedToggle() == null) {
            button.setSelected(true);
        }
        return button;
    }

    private void updateBadge() {
        if (noticesButton == null) {
            return;
        }
        try {
            int unread = portal.unreadCount();
            noticesButton.setText(unread == 0 ? "Notifications" : "Notifications (" + unread + ")");
        } catch (LibraryException e) {
            LOG.log(Level.WARNING, "Could not count the unread notifications", e);
        }
    }

    private void changePassword() {
        new PasswordDialog(getScene().getWindow()).showAndWait().ifPresent(passwords -> {
            try {
                auth.changeMemberPassword(portal.member(), passwords.current(), passwords.replacement());
                status.set("Password changed");
            } catch (LibraryException e) {
                Dialogs.error(getScene().getWindow(), e.getMessage());
            } finally {
                Arrays.fill(passwords.current(), ' ');
                Arrays.fill(passwords.replacement(), ' ');
            }
        });
    }
}
