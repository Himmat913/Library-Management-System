package com.ucs553.library.ui;

import com.ucs553.library.model.Book;
import com.ucs553.library.model.DigitalResource;
import com.ucs553.library.model.Item;
import com.ucs553.library.model.ItemType;
import com.ucs553.library.model.Magazine;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;

class ItemDialog extends Dialog<Item> {

    private final Item existing;
    private final ComboBox<ItemType> type = new ComboBox<>(FXCollections.observableArrayList(ItemType.values()));
    private final TextField title = new TextField();
    private final TextField author = new TextField();
    private final TextField year = new TextField();
    private final TextField copies = new TextField("1");
    private final ComboBox<String> category = new ComboBox<>();
    private final TextField isbn = new TextField();
    private final TextField pages = new TextField();
    private final TextField issue = new TextField();
    private final TextField publisher = new TextField();
    private final TextField format = new TextField();
    private final TextField size = new TextField();
    private final Label error = new Label();
    private Item result;

    ItemDialog(Window owner, Item existing, List<String> categories) {
        this.existing = existing;
        initOwner(owner);
        setTitle(existing == null ? "Add item" : "Edit item");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane common = grid();
        addRow(common, 0, "Type", type);
        addRow(common, 1, "Title", title);
        addRow(common, 2, "Author", author);
        addRow(common, 3, "Year", year);
        addRow(common, 4, "Copies", copies);
        category.setEditable(true);
        category.setPrefWidth(280);
        category.getItems().setAll(categories);
        category.setValue(Item.DEFAULT_CATEGORY);
        addRow(common, 5, "Category", category);

        GridPane bookPane = grid();
        addRow(bookPane, 0, "ISBN", isbn);
        addRow(bookPane, 1, "Pages", pages);
        GridPane magazinePane = grid();
        addRow(magazinePane, 0, "Issue number", issue);
        addRow(magazinePane, 1, "Publisher", publisher);
        GridPane digitalPane = grid();
        addRow(digitalPane, 0, "Format", format);
        addRow(digitalPane, 1, "Size (MB)", size);

        showOnlyFor(bookPane, ItemType.BOOK);
        showOnlyFor(magazinePane, ItemType.MAGAZINE);
        showOnlyFor(digitalPane, ItemType.DIGITAL);

        error.setStyle("-fx-text-fill: #b91c1c;");
        error.setWrapText(true);
        VBox content = new VBox(12, common, bookPane, magazinePane, digitalPane, error);
        content.setPadding(new Insets(16));
        content.setPrefWidth(420);
        getDialogPane().setContent(content);

        type.setValue(ItemType.BOOK);
        if (existing != null) {
            fill(existing);
            type.setDisable(true);
        }
        type.valueProperty().addListener((obs, old, now) -> Platform.runLater(() -> {
            if (getDialogPane().getScene() != null) {
                getDialogPane().getScene().getWindow().sizeToScene();
            }
        }));

        getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            try {
                result = build();
            } catch (IllegalArgumentException e) {
                error.setText(e.getMessage());
                event.consume();
            }
        });
        setResultConverter(button -> button == ButtonType.OK ? result : null);
    }

    private void fill(Item item) {
        type.setValue(item.getType());
        title.setText(item.getTitle());
        author.setText(item.getAuthor());
        year.setText(String.valueOf(item.getYear()));
        copies.setText(String.valueOf(item.getTotalCopies()));
        category.setValue(item.getCategory());
        if (item instanceof Book b) {
            isbn.setText(b.getIsbn());
            pages.setText(String.valueOf(b.getPages()));
        } else if (item instanceof Magazine m) {
            issue.setText(String.valueOf(m.getIssueNumber()));
            publisher.setText(m.getPublisher());
        } else if (item instanceof DigitalResource d) {
            format.setText(d.getFormat());
            size.setText(String.valueOf(d.getSizeMb()));
        }
    }

    private Item build() {
        String itemTitle = required(title, "Title");
        String itemAuthor = required(author, "Author");
        int itemYear = whole(year, "Year");
        if (itemYear < 1000 || itemYear > 2100) {
            throw new IllegalArgumentException("Year must be between 1000 and 2100");
        }
        int total = whole(copies, "Copies");
        if (total < 1) {
            throw new IllegalArgumentException("There must be at least one copy");
        }

        Item item = switch (type.getValue()) {
            case BOOK -> new Book.Builder().title(itemTitle).author(itemAuthor).year(itemYear).copies(total)
                    .isbn(isbn.getText()).pages(optionalWhole(pages, "Pages")).build();
            case MAGAZINE -> new Magazine(itemTitle, itemAuthor, itemYear, total,
                    optionalWhole(issue, "Issue number"), publisher.getText());
            case DIGITAL -> new DigitalResource(itemTitle, itemAuthor, itemYear, total,
                    format.getText(), optionalDecimal(size, "Size"));
        };

        item.setCategory(category.getEditor().getText());

        if (existing != null) {
            int onLoan = existing.getTotalCopies() - existing.getAvailableCopies();
            if (total < onLoan) {
                throw new IllegalArgumentException("Copies cannot be fewer than the " + onLoan + " currently on loan");
            }
            item.setId(existing.getId());
            item.restoreAvailableCopies(total - onLoan);
        }
        return item;
    }

    private void showOnlyFor(GridPane pane, ItemType shownFor) {
        pane.visibleProperty().bind(type.valueProperty().isEqualTo(shownFor));
        pane.managedProperty().bind(pane.visibleProperty());
    }

    private static GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(8);
        return g;
    }

    private static void addRow(GridPane g, int row, String label, javafx.scene.Node field) {
        g.add(new Label(label), 0, row);
        g.add(field, 1, row);
        if (field instanceof TextField tf) {
            tf.setPrefWidth(280);
        }
    }

    private static String required(TextField field, String name) {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return text;
    }

    private static int whole(TextField field, String name) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a whole number");
        }
    }

    private static int optionalWhole(TextField field, String name) {
        return field.getText().isBlank() ? 0 : whole(field, name);
    }

    private static double optionalDecimal(TextField field, String name) {
        if (field.getText().isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(field.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a number");
        }
    }
}
