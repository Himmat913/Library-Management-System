package com.ucs553.library.ui;

import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.LoanStatus;
import com.ucs553.library.model.ReservationStatus;
import com.ucs553.library.service.FineDetail;
import com.ucs553.library.service.LoanDetail;
import com.ucs553.library.service.MemberPortal;
import com.ucs553.library.service.ReservationDetail;
import javafx.beans.binding.Bindings;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

// everything that belongs to the signed-in member: loans, reservations and fines
public class MemberAccountView extends BorderPane implements Refreshable {

    private static final Logger LOG = Logger.getLogger(MemberAccountView.class.getName());

    private final MemberPortal portal;
    private final StringProperty status;
    private final ObservableList<LoanDetail> loans = FXCollections.observableArrayList();
    private final ObservableList<ReservationDetail> reservations = FXCollections.observableArrayList();
    private final ObservableList<FineDetail> fines = FXCollections.observableArrayList();
    private final TableView<LoanDetail> loanTable = new TableView<>();
    private final TableView<ReservationDetail> reservationTable = new TableView<>();
    private final TableView<FineDetail> fineTable = new TableView<>();
    private final CheckBox showReturned = new CheckBox("Show returned items too");
    private final Label fineTotal = new Label();

    public MemberAccountView(MemberPortal portal, StringProperty status) {
        this.portal = portal;
        this.status = status;
        setPadding(new Insets(16));

        TabPane tabs = new TabPane(
                tab("Loans", buildLoans()),
                tab("Reservations", buildReservations()),
                tab("Fines", buildFines()));
        setTop(Pages.header("My account", "Your loans, reservations and fines."));
        setCenter(tabs);
    }

    private static Tab tab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private VBox buildLoans() {
        loanTable.getColumns().add(Pages.<LoanDetail>text("Item", 280, LoanDetail::itemTitle));
        loanTable.getColumns().add(Pages.<LoanDetail>text("Issued", 110, d -> Pages.DATE.format(d.loan().getIssuedOn())));
        loanTable.getColumns().add(Pages.<LoanDetail>text("Due", 110, d -> Pages.DATE.format(d.loan().getDueOn())));
        loanTable.getColumns().add(Pages.<LoanDetail>text("Status", 100, d -> switch (d.status()) {
            case ACTIVE -> "On loan";
            case OVERDUE -> "Overdue";
            case RETURNED -> "Returned";
        }));
        loanTable.getColumns().add(Pages.<LoanDetail>text("Renewals", 80, d -> String.valueOf(d.loan().getRenewCount())));
        loanTable.getColumns().add(Pages.<LoanDetail>text("Fine so far", 100, d ->
                d.fine().signum() > 0 ? Pages.rupees(d.fine()) : "-"));
        loanTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        loanTable.setPlaceholder(new Label("You have nothing on loan"));
        loanTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(LoanDetail row, boolean empty) {
                super.updateItem(row, empty);
                getStyleClass().remove("overdue");
                if (!empty && row != null && row.status() == LoanStatus.OVERDUE) {
                    getStyleClass().add("overdue");
                }
            }
        });
        loanTable.setItems(loans);

        showReturned.selectedProperty().addListener((obs, was, now) -> refresh());
        Button renew = new Button("Renew selected");
        renew.getStyleClass().add("primary");
        renew.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            LoanDetail row = loanTable.getSelectionModel().getSelectedItem();
            return row == null || row.status() == LoanStatus.RETURNED;
        }, loanTable.getSelectionModel().selectedItemProperty()));
        renew.setOnAction(e -> renewSelected());

        return page(toolbar(showReturned, renew), loanTable);
    }

    private VBox buildReservations() {
        reservationTable.getColumns().add(Pages.<ReservationDetail>text("Item", 280, ReservationDetail::itemTitle));
        reservationTable.getColumns().add(Pages.<ReservationDetail>text("Reserved on", 110, d -> Pages.DATE.format(d.reservation().getReservedOn())));
        reservationTable.getColumns().add(Pages.<ReservationDetail>text("Status", 240, this::describe));
        reservationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        reservationTable.setPlaceholder(new Label("You have no reservations"));
        reservationTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ReservationDetail row, boolean empty) {
                super.updateItem(row, empty);
                getStyleClass().remove("ready");
                if (!empty && row != null && row.reservation().getStatus() == ReservationStatus.READY) {
                    getStyleClass().add("ready");
                }
            }
        });
        reservationTable.setItems(reservations);

        Button cancel = new Button("Cancel reservation");
        cancel.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            ReservationDetail row = reservationTable.getSelectionModel().getSelectedItem();
            return row == null || !row.reservation().getStatus().isOpen();
        }, reservationTable.getSelectionModel().selectedItemProperty()));
        cancel.setOnAction(e -> cancelSelected());
        Label note = new Label("When a copy is ready it is held for you for a few days. Collect it at the desk.");
        note.getStyleClass().add("muted");

        return page(toolbar(note, cancel), reservationTable);
    }

    private VBox buildFines() {
        fineTable.getColumns().add(Pages.<FineDetail>text("Item", 280, FineDetail::itemTitle));
        fineTable.getColumns().add(Pages.<FineDetail>text("Amount", 90, d -> Pages.rupees(d.fine().getAmount())));
        fineTable.getColumns().add(Pages.<FineDetail>text("Charged on", 110, d -> Pages.DATE.format(d.fine().getCreatedOn())));
        fineTable.getColumns().add(Pages.<FineDetail>text("Paid on", 110, d ->
                d.fine().isPaid() ? Pages.DATE.format(d.fine().getPaidOn()) : "Unpaid"));
        fineTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        fineTable.setPlaceholder(new Label("You have no fines"));
        fineTable.setItems(fines);
        fineTotal.getStyleClass().add("section-title");
        Label note = new Label("Fines are paid at the library desk.");
        note.getStyleClass().add("muted");
        return page(toolbar(fineTotal, note), fineTable);
    }

    private static HBox toolbar(javafx.scene.Node left, javafx.scene.Node right) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(12, left, spacer, right);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private static VBox page(HBox bar, TableView<?> table) {
        VBox box = new VBox(10, bar, table);
        box.setPadding(new Insets(12, 0, 0, 0));
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private String describe(ReservationDetail d) {
        ReservationStatus s = d.reservation().getStatus();
        return switch (s) {
            case READY -> "Ready, held for you until " + Pages.DATE.format(d.reservation().getHoldUntil());
            case WAITING -> "Waiting, number " + d.position() + " in the queue";
            default -> s.name().charAt(0) + s.name().substring(1).toLowerCase(Locale.ROOT);
        };
    }

    @Override
    public void refresh() {
        try {
            loans.setAll(portal.loans().stream()
                    .filter(d -> showReturned.isSelected() || d.status() != LoanStatus.RETURNED).toList());
            reservations.setAll(portal.reservations());
            fines.setAll(portal.fines());
            fineTotal.setText("Unpaid: " + Pages.rupees(portal.unpaidTotal()));
            status.set(loans.size() + " loans, " + reservations.stream().filter(r -> r.reservation().getStatus().isOpen()).count()
                    + " open reservations");
        } catch (LibraryException e) {
            fail("Could not load your account", e);
        }
    }

    private void renewSelected() {
        LoanDetail row = loanTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            return;
        }
        try {
            var loan = portal.renew(row.loan().getId());
            refresh();
            status.set("Renewed \"" + row.itemTitle() + "\", now due " + Pages.DATE.format(loan.getDueOn()));
        } catch (LibraryException e) {
            fail(e.getMessage(), e);
        }
    }

    private void cancelSelected() {
        ReservationDetail row = reservationTable.getSelectionModel().getSelectedItem();
        if (row == null || !Dialogs.confirm(window(), "Cancel your reservation for \"" + row.itemTitle() + "\"?")) {
            return;
        }
        try {
            portal.cancelReservation(row.reservation().getId());
            refresh();
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
