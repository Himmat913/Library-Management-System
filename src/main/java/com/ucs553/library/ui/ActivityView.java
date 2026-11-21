package com.ucs553.library.ui;

import com.ucs553.library.dao.DataAccessException;
import com.ucs553.library.model.AuditAction;
import com.ucs553.library.model.AuditEntry;
import com.ucs553.library.service.LibraryService;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// the audit trail: who issued, returned, fined or changed what, and when
public class ActivityView extends BorderPane implements Refreshable {

    private static final Logger LOG = Logger.getLogger(ActivityView.class.getName());
    private static final int LIMIT = 1000;

    private final LibraryService service;
    private final StringProperty status;
    private final ObservableList<AuditEntry> rows = FXCollections.observableArrayList();
    private final FilteredList<AuditEntry> filtered = new FilteredList<>(rows, e -> true);
    private final TableView<AuditEntry> table = new TableView<>();
    private final ComboBox<AuditAction> actionBox = new ComboBox<>();
    private final TextField search = new TextField();

    public ActivityView(LibraryService service, StringProperty status) {
        this.service = service;
        this.status = status;
        setPadding(new Insets(16));

        actionBox.getItems().add(null);
        actionBox.getItems().addAll(AuditAction.values());
        actionBox.setPromptText("All actions");
        actionBox.setButtonCell(actionCell());
        actionBox.setCellFactory(lv -> actionCell());
        actionBox.valueProperty().addListener((obs, old, now) -> applyFilter());
        search.setPromptText("Search who or what");
        search.setPrefWidth(260);
        search.textProperty().addListener((obs, old, now) -> applyFilter());

        Button export = new Button("Export CSV");
        export.setOnAction(e -> exportCsv());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, search, actionBox, spacer, export);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 12, 0));

        table.getColumns().add(Pages.<AuditEntry>text("When", 150, e -> Pages.DATE_TIME.format(e.getAt())));
        table.getColumns().add(Pages.<AuditEntry>text("Who", 150, AuditEntry::getActor));
        table.getColumns().add(Pages.<AuditEntry>text("Action", 150, e -> e.getAction().toString()));
        table.getColumns().add(Pages.<AuditEntry>text("Details", 420, AuditEntry::getDetails));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(Pages.empty("Nothing recorded yet", "Issues, returns and fines will be listed here."));
        table.setItems(filtered);

        setTop(new VBox(Pages.header("Activity log",
                "Every issue, return, fine, reservation and account change, with who did it and when."), toolbar));
        setCenter(table);
    }

    private ListCell<AuditAction> actionCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(AuditAction action, boolean empty) {
                super.updateItem(action, empty);
                setText(empty ? null : action == null ? "All actions" : action.toString());
            }
        };
    }

    private void applyFilter() {
        AuditAction action = actionBox.getValue();
        String q = search.getText().trim().toLowerCase();
        filtered.setPredicate(e -> (action == null || e.getAction() == action)
                && (q.isEmpty() || e.getActor().toLowerCase().contains(q) || e.getDetails().toLowerCase().contains(q)));
        status.set(filtered.size() + " of " + rows.size() + " log entries");
    }

    @Override
    public void refresh() {
        try {
            rows.setAll(service.recentActivity(LIMIT));
            applyFilter();
        } catch (DataAccessException e) {
            LOG.log(Level.WARNING, "Could not load the activity log", e);
            Dialogs.error(window(), "Could not load the activity log");
        }
    }

    private void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export activity log");
        chooser.setInitialFileName("activity-log.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        List<String> lines = new ArrayList<>();
        lines.add("when,who,action,details");
        for (AuditEntry entry : filtered) {
            lines.add(String.join(",", quote(entry.getAt().toString()), quote(entry.getActor()),
                    quote(entry.getAction().toString()), quote(entry.getDetails())));
        }
        try {
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
            status.set("Saved " + file.getAbsolutePath());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not write the activity log", e);
            Dialogs.error(window(), "Could not write the file: " + e.getMessage());
        }
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private Window window() {
        return getScene().getWindow();
    }
}
