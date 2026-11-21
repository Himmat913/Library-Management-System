package com.ucs553.library.ui;

import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.AuditAction;
import com.ucs553.library.model.Member;
import com.ucs553.library.service.AuthService;
import com.ucs553.library.service.LibraryService;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

// admins use this to give members a login, or to reset one they forgot
public class AccountsView extends BorderPane implements Refreshable {

    private static final Logger LOG = Logger.getLogger(AccountsView.class.getName());

    private final LibraryService service;
    private final AuthService auth;
    private final StringProperty status;
    private final ObservableList<Member> data = FXCollections.observableArrayList();
    private final FilteredList<Member> filtered = new FilteredList<>(data, m -> true);
    private final TableView<Member> table = new TableView<>();
    private final TextField search = new TextField();
    private final Set<Integer> withAccount = new HashSet<>();

    public AccountsView(LibraryService service, AuthService auth, StringProperty status) {
        this.service = service;
        this.auth = auth;
        this.status = status;
        setPadding(new Insets(16));

        search.setPromptText("Search by name or email");
        search.setPrefWidth(300);
        search.textProperty().addListener((obs, old, text) -> {
            String q = text.trim().toLowerCase();
            filtered.setPredicate(m -> q.isEmpty() || m.getName().toLowerCase().contains(q)
                    || m.getEmail().toLowerCase().contains(q));
        });

        Button issue = new Button("Create or reset login");
        issue.getStyleClass().add("primary");
        issue.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        issue.setOnAction(e -> issueLogin());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, search, spacer, issue);
        toolbar.setPadding(new Insets(0, 0, 12, 0));

        table.getColumns().add(Pages.<Member>text("Name", 200, Member::getName));
        table.getColumns().add(Pages.<Member>text("Sign-in email", 260, Member::getEmail));
        table.getColumns().add(Pages.<Member>text("Login", 120, m -> withAccount.contains(m.getId()) ? "Has a login" : "No login yet"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No members to show"));
        SortedList<Member> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        setTop(new VBox(Pages.header("Member logins",
                "Members sign in with their email. A new or reset login gets a temporary password that must be changed at first sign in."),
                toolbar));
        setCenter(table);
    }

    @Override
    public void refresh() {
        try {
            withAccount.clear();
            withAccount.addAll(auth.membersWithAccounts());
            data.setAll(service.allMembers());
            table.refresh();
            status.set(withAccount.size() + " of " + data.size() + " members have a login");
        } catch (LibraryException e) {
            fail("Could not load the members", e);
        }
    }

    private void issueLogin() {
        Member member = table.getSelectionModel().getSelectedItem();
        if (member == null) {
            return;
        }
        boolean reset = withAccount.contains(member.getId());
        if (reset && !Dialogs.confirm(window(),
                "Reset the password for " + member.getName() + "? Their current password will stop working.")) {
            return;
        }
        try {
            String temporary = auth.createOrResetMemberAccount(member.getId());
            service.recordAccountEvent(reset ? AuditAction.PASSWORD_RESET : AuditAction.ACCOUNT_CREATED,
                    (reset ? "Password reset for " : "Login created for ") + member.getName());
            refresh();
            showTemporaryPassword(member, temporary);
        } catch (LibraryException e) {
            fail(e.getMessage(), e);
        }
    }

    private void showTemporaryPassword(Member member, String temporary) {
        TextField field = new TextField(temporary);
        field.setEditable(false);
        field.setStyle("-fx-font-size: 20px; -fx-font-family: Consolas, monospace;");
        Button copy = new Button("Copy");
        copy.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(temporary);
            Clipboard.getSystemClipboard().setContent(content);
        });
        Label note = new Label("Give this to " + member.getName() + " (sign in with " + member.getEmail()
                + "). It is shown only now, and they must choose a new password at first sign in.");
        note.setWrapText(true);
        HBox row = new HBox(8, field, copy);
        HBox.setHgrow(field, Priority.ALWAYS);
        VBox content = new VBox(12, note, row);
        content.setPadding(new Insets(16));
        content.setPrefWidth(420);

        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(window());
        dialog.setTitle("Temporary password");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void fail(String message, Exception e) {
        LOG.log(Level.WARNING, message, e);
        Dialogs.error(window(), message);
    }

    private Window window() {
        return getScene().getWindow();
    }
}
