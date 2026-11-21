package com.ucs553.library.ui;

import com.ucs553.library.Session;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.Item;
import com.ucs553.library.model.Member;
import com.ucs553.library.service.LibraryService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;
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
    private final ComboBox<String> categoryFilter = new ComboBox<>();
    private final Label details = new Label();

    public ItemsView(LibraryService service, StringProperty status) {
        this.service = service;
        this.status = status;
        setPadding(new Insets(16));

        search.setPromptText("Search by title, author, type or category");
        search.setPrefWidth(300);
        categoryFilter.setPromptText("All categories");
        categoryFilter.setPrefWidth(170);
        Button clear = new Button("Clear");
        clear.setOnAction(e -> {
            search.clear();
            categoryFilter.setValue(null);
        });
        Button reserve = new Button("Reserve for member");
        reserve.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            Item selected = table.getSelectionModel().getSelectedItem();
            return selected == null || selected.isAvailable();
        }, table.getSelectionModel().selectedItemProperty()));
        reserve.setOnAction(e -> reserveSelected());
        Button add = new Button("Add item");
        Button edit = new Button("Edit");
        Button delete = new Button("Delete");
        add.getStyleClass().add("primary");
        delete.getStyleClass().add("danger");
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        delete.setVisible(Session.isAdmin());
        delete.setManaged(Session.isAdmin());
        add.setOnAction(e -> addItem());
        edit.setOnAction(e -> editSelected());
        delete.setOnAction(e -> deleteSelected());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, search, categoryFilter, clear, spacer, reserve, add, edit, delete);
        toolbar.setPadding(new Insets(0, 0, 12, 0));
        setTop(new VBox(Pages.header("Items", "Everything the library lends: books, magazines and digital resources."), toolbar));

        buildTable();
        setCenter(table);

        details.getStyleClass().add("details");
        details.setPadding(new Insets(10, 0, 0, 0));
        setBottom(details);

        search.textProperty().addListener((obs, old, text) -> applyFilter());
        categoryFilter.valueProperty().addListener((obs, old, now) -> applyFilter());
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, item) ->
                details.setText(item == null ? "" : item.describe()));
    }

    private void applyFilter() {
        String q = search.getText().trim().toLowerCase();
        String category = categoryFilter.getValue();
        filtered.setPredicate(i -> (q.isEmpty()
                || i.getTitle().toLowerCase().contains(q)
                || i.getAuthor().toLowerCase().contains(q)
                || i.getCategory().toLowerCase().contains(q)
                || i.getType().toString().toLowerCase().contains(q))
                && (category == null || i.getCategory().equalsIgnoreCase(category)));
        updateStatus();
    }

    private void buildTable() {
        TableColumn<Item, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getType().toString()));
        type.setPrefWidth(130);

        TableColumn<Item, String> title = new TableColumn<>("Title");
        title.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getTitle()));
        title.setPrefWidth(320);

        TableColumn<Item, String> category = new TableColumn<>("Category");
        category.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCategory()));
        category.setPrefWidth(130);

        TableColumn<Item, String> author = new TableColumn<>("Author");
        author.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getAuthor()));
        author.setPrefWidth(200);

        TableColumn<Item, Integer> year = new TableColumn<>("Year");
        year.setCellValueFactory(c -> new ReadOnlyObjectWrapper<Integer>(c.getValue().getYear()));
        year.setPrefWidth(80);

        TableColumn<Item, String> copies = Pages.<Item>chip("Available", 100,
                i -> i.getAvailableCopies() + " / " + i.getTotalCopies(),
                i -> i.isAvailable() ? "success" : "danger");

        table.getColumns().add(type);
        table.getColumns().add(title);
        table.getColumns().add(category);
        table.getColumns().add(author);
        table.getColumns().add(year);
        table.getColumns().add(copies);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(Pages.empty("No items match", "Try a different search or category."));

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
            String kept = categoryFilter.getValue();
            categoryFilter.getItems().setAll(service.categories());
            categoryFilter.setValue(categoryFilter.getItems().contains(kept) ? kept : null);
            updateStatus();
        } catch (LibraryException e) {
            fail("Could not load the items", e);
        }
    }

    private void updateStatus() {
        status.set(filtered.size() + " of " + data.size() + " items");
    }

    private void addItem() {
        new ItemDialog(window(), null, categories()).showAndWait().ifPresent(this::save);
    }

    private void editSelected() {
        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            new ItemDialog(window(), selected, categories()).showAndWait().ifPresent(this::save);
        }
    }

    private List<String> categories() {
        return List.copyOf(categoryFilter.getItems());
    }

    private void reserveSelected() {
        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            List<Member> members = service.allMembers().stream().sorted().toList();
            ChoiceDialog<Member> dialog = new ChoiceDialog<>(null, members);
            dialog.initOwner(window());
            dialog.setTitle("Reserve");
            dialog.setHeaderText(null);
            dialog.setContentText("Reserve \"" + selected.getTitle() + "\" for:");
            dialog.showAndWait().ifPresent(member -> {
                try {
                    service.reserve(selected.getId(), member.getId());
                    status.set(member.getName() + " is now in the queue for \"" + selected.getTitle() + "\"");
                } catch (LibraryException e) {
                    fail(e.getMessage(), e);
                }
            });
        } catch (LibraryException e) {
            fail("Could not load the members", e);
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
