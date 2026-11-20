package com.ucs553.library.ui;

import com.ucs553.library.Session;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.Item;
import com.ucs553.library.service.LibraryService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Window;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemsView extends BorderPane implements Refreshable {

    private static final Logger LOG = Logger.getLogger(ItemsView.class.getName());

    private final LibraryService service;
    private final StringProperty status;
    private final ObservableList<Item> data = FXCollections.observableArrayList();
    private final FilteredList<Item> filtered = new FilteredList<>(data, i -> true);
    private final TableView<Item> table = new TableView<>();
    private final TextField search = new TextField();
    private final Label details = new Label();

    public ItemsView(LibraryService service, StringProperty status) {
        this.service = service;
        this.status = status;
        setPadding(new Insets(16));

        search.setPromptText("Search by title, author or type");
        search.setPrefWidth(300);
        Button add = new Button("Add item");
        Button edit = new Button("Edit");
        Button delete = new Button("Delete");
        add.getStyleClass().add("primary");
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        delete.setVisible(Session.isAdmin());
        delete.setManaged(Session.isAdmin());
        add.setOnAction(e -> addItem());
        edit.setOnAction(e -> editSelected());
        delete.setOnAction(e -> deleteSelected());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, search, spacer, add, edit, delete);
        toolbar.setPadding(new Insets(0, 0, 12, 0));
        setTop(toolbar);

        buildTable();
        setCenter(table);

        details.getStyleClass().add("details");
        details.setPadding(new Insets(10, 0, 0, 0));
        setBottom(details);

        search.textProperty().addListener((obs, old, text) -> {
            String q = text.trim().toLowerCase();
            filtered.setPredicate(i -> q.isEmpty()
                    || i.getTitle().toLowerCase().contains(q)
                    || i.getAuthor().toLowerCase().contains(q)
                    || i.getType().toString().toLowerCase().contains(q));
            updateStatus();
        });
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, item) ->
                details.setText(item == null ? "" : item.describe()));
    }

    private void buildTable() {
        TableColumn<Item, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getType().toString()));
        type.setPrefWidth(130);

        TableColumn<Item, String> title = new TableColumn<>("Title");
        title.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getTitle()));
        title.setPrefWidth(320);

        TableColumn<Item, String> author = new TableColumn<>("Author");
        author.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getAuthor()));
        author.setPrefWidth(200);

        TableColumn<Item, Integer> year = new TableColumn<>("Year");
        year.setCellValueFactory(c -> new ReadOnlyObjectWrapper<Integer>(c.getValue().getYear()));
        year.setPrefWidth(80);

        TableColumn<Item, String> copies = new TableColumn<>("Available");
        copies.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getAvailableCopies() + " / " + c.getValue().getTotalCopies()));
        copies.setPrefWidth(100);

        table.getColumns().add(type);
        table.getColumns().add(title);
        table.getColumns().add(author);
        table.getColumns().add(year);
        table.getColumns().add(copies);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No items to show"));

        SortedList<Item> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.getSortOrder().add(title);

        table.setRowFactory(tv -> {
            TableRow<Item> row = new TableRow<>() {
                @Override
                protected void updateItem(Item item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().remove("unavailable");
                    if (!empty && item != null && !item.isAvailable()) {
                        getStyleClass().add("unavailable");
                    }
                }
            };
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    editSelected();
                }
            });
            return row;
        });
    }

    @Override
    public void refresh() {
        try {
            data.setAll(service.allItems());
            updateStatus();
        } catch (LibraryException e) {
            fail("Could not load the items", e);
        }
    }

    private void updateStatus() {
        status.set(filtered.size() + " of " + data.size() + " items");
    }

    private void addItem() {
        new ItemDialog(window(), null).showAndWait().ifPresent(this::save);
    }

    private void editSelected() {
        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            new ItemDialog(window(), selected).showAndWait().ifPresent(this::save);
        }
    }

    private void deleteSelected() {
        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (!Dialogs.confirm(window(), "Delete \"" + selected.getTitle() + "\"?")) {
            return;
        }
        try {
            service.deleteItem(selected.getId());
            refresh();
        } catch (LibraryException e) {
            fail("This item could not be deleted. It may still have loan records.", e);
        }
    }

    private void save(Item item) {
        try {
            service.saveItem(item);
            refresh();
        } catch (LibraryException e) {
            fail("Could not save the item", e);
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
