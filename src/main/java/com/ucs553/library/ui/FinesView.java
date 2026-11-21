package com.ucs553.library.ui;

import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.service.FineDetail;
import com.ucs553.library.service.LibraryService;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FinesView extends BorderPane implements Refreshable {

    private static final Logger LOG = Logger.getLogger(FinesView.class.getName());

    private final LibraryService service;
    private final StringProperty status;
    private final ObservableList<FineDetail> rows = FXCollections.observableArrayList();
    private final TableView<FineDetail> table = new TableView<>(rows);
    private final CheckBox unpaidOnly = new CheckBox("Unpaid only");
    private final Label total = new Label();

    public FinesView(LibraryService service, StringProperty status) {
        this.service = service;
        this.status = status;
        setPadding(new Insets(16));

        unpaidOnly.setSelected(true);
        unpaidOnly.selectedProperty().addListener((obs, was, now) -> refresh());
        total.getStyleClass().add("section-title");

        Button collect = new Button("Collect fine");
        collect.getStyleClass().add("primary");
        collect.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        collect.setOnAction(e -> collectSelected());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(16, unpaidOnly, total, spacer, collect);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 12, 0));

        table.getColumns().add(Pages.<FineDetail>text("Member", 180, f -> f.memberName()));
        table.getColumns().add(Pages.<FineDetail>text("Item", 260, f -> f.itemTitle()));
        table.getColumns().add(Pages.<FineDetail>text("Amount", 90, f -> Pages.rupees(f.fine().getAmount())));
        table.getColumns().add(Pages.<FineDetail>text("Charged on", 110, f -> Pages.DATE.format(f.fine().getCreatedOn())));
        table.getColumns().add(Pages.<FineDetail>chip("Status", 140,
                f -> f.fine().isPaid() ? "Paid " + Pages.DATE.format(f.fine().getPaidOn()) : "Unpaid",
                f -> f.fine().isPaid() ? "success" : "danger"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(Pages.empty("No fines to show", "Fines appear when an item comes back late."));
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(FineDetail row, boolean empty) {
                super.updateItem(row, empty);
                getStyleClass().remove("overdue");
                if (!empty && row != null && !row.fine().isPaid()) {
                    getStyleClass().add("overdue");
                }
            }
        });

        setTop(new VBox(Pages.header("Fines", "Late fees charged when items come back. Members with too much unpaid cannot borrow."),
                toolbar));
        setCenter(table);
    }

    @Override
    public void refresh() {
        try {
            rows.setAll(service.fineDetails(unpaidOnly.isSelected()));
            BigDecimal unpaid = rows.stream().filter(f -> !f.fine().isPaid())
                    .map(f -> f.fine().getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
            total.setText("Outstanding in this list: " + Pages.rupees(unpaid));
            status.set(rows.size() + " fines shown");
        } catch (LibraryException e) {
            fail("Could not load the fines", e);
        }
    }

    private void collectSelected() {
        FineDetail row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            return;
        }
        if (row.fine().isPaid()) {
            Dialogs.info(window(), "This fine was already paid on " + Pages.DATE.format(row.fine().getPaidOn()));
            return;
        }
        if (!Dialogs.confirm(window(), "Collect " + Pages.rupees(row.fine().getAmount()) + " from " + row.memberName() + "?")) {
            return;
        }
        try {
            service.collectFine(row.fine().getId());
            refresh();
            status.set("Collected " + Pages.rupees(row.fine().getAmount()) + " from " + row.memberName());
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
