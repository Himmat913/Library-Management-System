package com.ucs553.library.ui;

import com.ucs553.library.Session;
import com.ucs553.library.dao.DataAccessException;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.Staff;
import com.ucs553.library.service.AuthService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Window;

import java.util.logging.Level;
import java.util.logging.Logger;

public class StaffView extends BorderPane implements Refreshable {

    private static final Logger LOG = Logger.getLogger(StaffView.class.getName());

    private final AuthService auth;
    private final StringProperty status;
    private final ObservableList<Staff> data = FXCollections.observableArrayList();
    private final TableView<Staff> table = new TableView<>(data);

    public StaffView(AuthService auth, StringProperty status) {
        this.auth = auth;
        this.status = status;
        setPadding(new Insets(16));

        Button add = new Button("Add staff");
        add.getStyleClass().add("primary");
        Button delete = new Button("Delete");
        delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        add.setOnAction(e -> addStaff());
        delete.setOnAction(e -> deleteSelected());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, spacer, add, delete);
        toolbar.setPadding(new Insets(0, 0, 12, 0));
        setTop(toolbar);

        TableColumn<Staff, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getName()));
        TableColumn<Staff, String> username = new TableColumn<>("Username");
        username.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getUsername()));
        TableColumn<Staff, String> email = new TableColumn<>("Email");
        email.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getEmail()));
        TableColumn<Staff, String> role = new TableColumn<>("Role");
        role.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getRole().name()));
        table.getColumns().add(name);
        table.getColumns().add(username);
        table.getColumns().add(email);
        table.getColumns().add(role);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        setCenter(table);
    }

    @Override
    public void refresh() {
        try {
            data.setAll(auth.allStaff());
            status.set(data.size() + " staff accounts");
        } catch (DataAccessException e) {
            fail("Could not load the staff accounts", e);
        }
    }

    private void addStaff() {
        new StaffDialog(window()).showAndWait().ifPresent(form -> {
            try {
                auth.createStaff(form.name(), form.email(), form.username(), form.role(), form.password());
                refresh();
            } catch (DataAccessException e) {
                fail("Could not save the account", e);
            } catch (LibraryException e) {
                fail(e.getMessage(), e);
            }
        });
    }

    private void deleteSelected() {
        Staff selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || !Dialogs.confirm(window(), "Delete the account " + selected.getUsername() + "?")) {
            return;
        }
        try {
            auth.deleteStaff(selected.getId(), Session.current());
            refresh();
        } catch (DataAccessException e) {
            fail("Could not delete the account", e);
        } catch (LibraryException e) {
            fail(e.getMessage(), e);
        }
    }

    private void fail(String message, Exception e) {
        LOG.log(Level.WARNING, message, e);
        Dialogs.error(window(), message);
    }

    private Window window() {
        return getScene().getWindow();
    }
}
