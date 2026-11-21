package com.ucs553.library.ui;

import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.Item;
import com.ucs553.library.service.MemberPortal;
import javafx.beans.binding.Bindings;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MemberCatalogueView extends BorderPane implements Refreshable {

    private static final Logger LOG = Logger.getLogger(MemberCatalogueView.class.getName());

    private final MemberPortal portal;
    private final StringProperty status;
    private final ObservableList<Item> data = FXCollections.observableArrayList();
    private final TableView<Item> table = new TableView<>(data);
    private final TextField search = new TextField();
    private final ComboBox<String> categoryBox = new ComboBox<>();
    private final Label details = new Label();

    public MemberCatalogueView(MemberPortal portal, StringProperty status) {
        this.portal = portal;
        this.status = status;
        setPadding(new Insets(16));

        search.setPromptText("Search by title, author or category");
        search.setPrefWidth(320);
        categoryBox.setPromptText("All categories");
        categoryBox.setPrefWidth(170);
        Button clear = new Button("Clear");
        clear.setOnAction(e -> {
            search.clear();
            categoryBox.setValue(null);
        });
        search.textProperty().addListener((obs, old, now) -> load());
        categoryBox.valueProperty().addListener((obs, old, now) -> load());

        Button reserve = new Button("Reserve");
        reserve.getStyleClass().add("primary");
        reserve.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            Item selected = table.getSelectionModel().getSelectedItem();
            return selected == null || selected.isAvailable();
        }, table.getSelectionModel().selectedItemProperty()));
        reserve.setOnAction(e -> reserveSelected());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, search, categoryBox, clear, spacer, reserve);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 12, 0));

        table.getColumns().add(Pages.<Item>text("Title", 280, Item::getTitle));
        table.getColumns().add(Pages.<Item>text("Author", 190, Item::getAuthor));
        table.getColumns().add(Pages.<Item>text("Category", 120, Item::getCategory));
        table.getColumns().add(Pages.<Item>text("Type", 100, i -> i.getType().toString()));
        table.getColumns().add(Pages.<Item>text("Availability", 190, this::availability));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Nothing matches your search"));
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("unavailable");
                if (!empty && item != null && !item.isAvailable()) {
                    getStyleClass().add("unavailable");
                }
            }
        });
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, item) ->
                details.setText(item == null ? "" : item.describe()));
        details.getStyleClass().add("details");
        details.setPadding(new Insets(10, 0, 0, 0));

        setTop(new VBox(Pages.header("Catalogue",
                "Find something to read. Borrow at the desk, or reserve it here when every copy is out."), toolbar));
        setCenter(table);
        setBottom(details);
    }

    private String availability(Item item) {
        if (item.isAvailable()) {
            return item.getAvailableCopies() + " of " + item.getTotalCopies() + " available";
        }
        try {
            int waiting = portal.waitingCount(item.getId());
            return waiting == 0 ? "All copies out" : "All copies out, " + waiting + " waiting";
        } catch (LibraryException e) {
            return "All copies out";
        }
    }

    @Override
    public void refresh() {
        try {
            String kept = categoryBox.getValue();
            categoryBox.getItems().setAll(portal.categories());
            categoryBox.setValue(categoryBox.getItems().contains(kept) ? kept : null);
        } catch (LibraryException e) {
            LOG.log(Level.WARNING, "Could not load the categories", e);
        }
        load();
    }

    private void load() {
        try {
            data.setAll(portal.catalogue(search.getText(), categoryBox.getValue()));
            status.set(data.size() + " items shown");
        } catch (LibraryException e) {
            fail("Could not load the catalogue", e);
        }
    }

    private void reserveSelected() {
        Item item = table.getSelectionModel().getSelectedItem();
        if (item == null) {
            return;
        }
        try {
            portal.reserve(item.getId());
            load();
            Dialogs.info(window(), "You are in the queue for \"" + item.getTitle()
                    + "\". You will get a notice when a copy is held for you.");
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
