package com.ucs553.library.ui;

import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.ReservationStatus;
import com.ucs553.library.service.LibraryService;
import com.ucs553.library.service.ReservationDetail;
import javafx.beans.binding.Bindings;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReservationsView extends BorderPane implements Refreshable {

    private static final Logger LOG = Logger.getLogger(ReservationsView.class.getName());

    private final LibraryService service;
    private final StringProperty status;
    private final ObservableList<ReservationDetail> rows = FXCollections.observableArrayList();
    private final FilteredList<ReservationDetail> filtered = new FilteredList<>(rows, r -> true);
    private final TableView<ReservationDetail> table = new TableView<>();

    public ReservationsView(LibraryService service, StringProperty status) {
        this.service = service;
        this.status = status;
        setPadding(new Insets(16));

        ToggleGroup group = new ToggleGroup();
        HBox segments = new HBox();
        segments.getChildren().addAll(
                segment(group, "All", r -> true, true),
                segment(group, "Ready for pickup", r -> r.reservation().getStatus() == ReservationStatus.READY, false),
                segment(group, "Waiting", r -> r.reservation().getStatus() == ReservationStatus.WAITING, false));
        group.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) {
                old.setSelected(true);
            }
        });

        Button handOver = new Button("Hand over");
        handOver.getStyleClass().add("primary");
        handOver.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            ReservationDetail row = table.getSelectionModel().getSelectedItem();
            return row == null || row.reservation().getStatus() != ReservationStatus.READY;
        }, table.getSelectionModel().selectedItemProperty()));
        Button cancel = new Button("Cancel reservation");
        cancel.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        Button expire = new Button("Release lapsed holds");
        handOver.setOnAction(e -> handOverSelected());
        cancel.setOnAction(e -> cancelSelected());
        expire.setOnAction(e -> releaseLapsed());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, segments, spacer, expire, cancel, handOver);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 12, 0));

        buildTable();
        setTop(new VBox(Pages.header("Reservations",
                "Members waiting for an item. When a copy comes back it is held for the first in line."), toolbar));
        setCenter(table);
    }

    private ToggleButton segment(ToggleGroup group, String label, Predicate<ReservationDetail> test, boolean selected) {
        ToggleButton button = new ToggleButton(label);
        button.setToggleGroup(group);
        button.getStyleClass().add("segment");
        button.selectedProperty().addListener((obs, was, now) -> {
            if (now) {
                filtered.setPredicate(test::test);
                updateStatus();
            }
        });
        button.setSelected(selected);
        return button;
    }

    private void buildTable() {
        table.getColumns().add(Pages.<ReservationDetail>text("Item", 260, r -> r.itemTitle()));
        table.getColumns().add(Pages.<ReservationDetail>text("Member", 170, r -> r.memberName()));
        table.getColumns().add(Pages.<ReservationDetail>text("Status", 150, r ->
                r.reservation().getStatus() == ReservationStatus.READY ? "Ready for pickup" : "Waiting, number " + r.position()));
        table.getColumns().add(Pages.<ReservationDetail>text("Reserved on", 110, r -> Pages.DATE.format(r.reservation().getReservedOn())));
        table.getColumns().add(Pages.<ReservationDetail>text("Held until", 110, r ->
                r.reservation().getHoldUntil() == null ? "-" : Pages.DATE.format(r.reservation().getHoldUntil())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Nobody is waiting for anything right now"));
        table.setItems(filtered);
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ReservationDetail row, boolean empty) {
                super.updateItem(row, empty);
                getStyleClass().remove("ready");
                if (!empty && row != null && row.reservation().getStatus() == ReservationStatus.READY) {
                    getStyleClass().add("ready");
                }
            }
        });
    }

    @Override
    public void refresh() {
        try {
            service.expireHolds();
            rows.setAll(service.reservationDetails());
            updateStatus();
        } catch (LibraryException e) {
            fail("Could not load the reservations", e);
        }
    }

    private void updateStatus() {
        status.set(filtered.size() + " of " + rows.size() + " open reservations");
    }

    private void handOverSelected() {
        ReservationDetail row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            return;
        }
        try {
            service.borrow(row.reservation().getItemId(), row.reservation().getMemberId());
            refresh();
            status.set("Handed \"" + row.itemTitle() + "\" to " + row.memberName());
        } catch (LibraryException e) {
            fail(e.getMessage(), e);
        }
    }

    private void cancelSelected() {
        ReservationDetail row = table.getSelectionModel().getSelectedItem();
        if (row == null || !Dialogs.confirm(window(),
                "Cancel " + row.memberName() + "'s reservation for \"" + row.itemTitle() + "\"?")) {
            return;
        }
        try {
            service.cancelReservation(row.reservation().getId());
            refresh();
        } catch (LibraryException e) {
            fail(e.getMessage(), e);
        }
    }

    private void releaseLapsed() {
        try {
            service.expireHolds();
            refresh();
            status.set("Holds that were not collected in time have been released");
        } catch (LibraryException e) {
            fail("Could not release the holds", e);
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
