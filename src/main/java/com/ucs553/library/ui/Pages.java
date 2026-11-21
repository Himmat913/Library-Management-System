package com.ucs553.library.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.function.Function;

// small helpers the screens share, so they all build headers and columns the same way
final class Pages {

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private Pages() {
    }

    static VBox header(String title, String subtitle) {
        Label heading = new Label(title);
        heading.getStyleClass().add("page-title");
        Label sub = new Label(subtitle);
        sub.getStyleClass().add("page-subtitle");
        sub.setWrapText(true);
        VBox box = new VBox(2, heading, sub);
        box.getStyleClass().add("page-header");
        return box;
    }

    static <T> TableColumn<T, String> text(String title, double width, Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(c -> new ReadOnlyStringWrapper(value.apply(c.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    // a column whose text is drawn as a coloured chip: kind is info, success, danger, warn or muted
    static <T> TableColumn<T, String> chip(String title, double width, Function<T, String> text, Function<T, String> kind) {
        TableColumn<T, String> column = text(title, width, text);
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                TableView<T> view = getTableView();
                int index = getIndex();
                if (empty || value == null || view == null || index < 0 || index >= view.getItems().size()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                T row = view.getItems().get(index);
                Label chip = new Label(value);
                chip.getStyleClass().addAll("chip", "chip-" + kind.apply(row));
                setGraphic(chip);
                setText(null);
            }
        });
        return column;
    }

    // what a table shows when it has nothing to list
    static VBox empty(String message, String hint) {
        Label title = new Label(message);
        title.getStyleClass().add("empty-title");
        Label sub = new Label(hint);
        sub.getStyleClass().add("empty-hint");
        VBox box = new VBox(4, title, sub);
        box.getStyleClass().add("empty-state");
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // a number with a caption underneath, for summary rows that are not buttons
    static VBox stat(String number, String caption, boolean warn) {
        Label big = new Label(number);
        big.getStyleClass().add("stat-number");
        Label small = new Label(caption);
        small.getStyleClass().add("stat-caption");
        VBox box = new VBox(2, big, small);
        box.getStyleClass().addAll("stat-box", "stat-static");
        if (warn) {
            box.getStyleClass().add("stat-warn");
        }
        return box;
    }

    static String rupees(java.math.BigDecimal amount) {
        return "₹" + amount.setScale(2);
    }
}
