package com.ucs553.library.ui;

import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.AuditEntry;
import com.ucs553.library.service.DashboardStats;
import com.ucs553.library.service.LibraryService;
import com.ucs553.library.service.PopularItem;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashboardView extends BorderPane implements Refreshable {

    private static final Logger LOG = Logger.getLogger(DashboardView.class.getName());

    private final LibraryService service;
    private final StringProperty status;
    private final Consumer<String> goTo;
    private final FlowPane stats = new FlowPane(12, 12);
    private final ListView<String> popular = new ListView<>();
    private final ListView<String> latest = new ListView<>();

    public DashboardView(LibraryService service, StringProperty status, Consumer<String> goTo) {
        this.service = service;
        this.status = status;
        this.goTo = goTo;
        setPadding(new Insets(16));

        popular.setPlaceholder(new Label("No loans yet"));
        latest.setPlaceholder(new Label("Nothing recorded yet"));
        HBox lists = new HBox(16, card("Most borrowed", popular), card("Latest activity", latest));
        lists.setPadding(new Insets(16, 0, 0, 0));
        VBox.setVgrow(lists, Priority.ALWAYS);

        VBox top = new VBox(Pages.header("Dashboard", "Where the library stands today. Click a box to jump to that page."), stats);
        setTop(top);
        setCenter(lists);
    }

    private VBox card(String title, ListView<String> list) {
        Label heading = new Label(title);
        heading.getStyleClass().add("section-title");
        VBox box = new VBox(8, heading, list);
        box.getStyleClass().add("card");
        VBox.setVgrow(list, Priority.ALWAYS);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private Button statBox(String number, String caption, String page, boolean warn) {
        Label big = new Label(number);
        big.getStyleClass().add("stat-number");
        Label small = new Label(caption);
        small.getStyleClass().add("stat-caption");
        VBox inside = new VBox(2, big, small);
        Button box = new Button();
        box.setGraphic(inside);
        box.getStyleClass().add("stat-box");
        if (warn) {
            box.getStyleClass().add("stat-warn");
        }
        box.setOnAction(e -> goTo.accept(page));
        return box;
    }

    @Override
    public void refresh() {
        try {
            service.expireHolds();
            DashboardStats s = service.stats();
            stats.getChildren().setAll(
                    statBox(String.valueOf(s.items()), "Items", "Items", false),
                    statBox(String.valueOf(s.members()), "Members", "Members", false),
                    statBox(String.valueOf(s.activeLoans()), "On loan", "Circulation", false),
                    statBox(String.valueOf(s.overdueLoans()), "Overdue", "Circulation", s.overdueLoans() > 0),
                    statBox(String.valueOf(s.openReservations()), "Reservations", "Reservations", false),
                    statBox(Pages.rupees(s.unpaidFines()), "Unpaid fines", "Fines", s.unpaidFines().signum() > 0));

            popular.getItems().clear();
            int rank = 1;
            for (PopularItem item : service.mostBorrowed(8)) {
                popular.getItems().add(rank++ + ". " + item.title() + " (" + item.loans() + " loans)");
            }
            latest.getItems().clear();
            for (AuditEntry entry : service.recentActivity(10)) {
                latest.getItems().add(Pages.DATE.format(entry.getAt()) + "  " + entry.getAction() + ": " + entry.getDetails());
            }
            status.set("Dashboard");
        } catch (LibraryException e) {
            LOG.log(Level.WARNING, "Could not build the dashboard", e);
            Dialogs.error(getScene().getWindow(), "Could not load the dashboard");
        }
    }
}
