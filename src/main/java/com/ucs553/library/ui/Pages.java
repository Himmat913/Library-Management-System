package com.ucs553.library.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
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

    static String rupees(java.math.BigDecimal amount) {
        return "₹" + amount.setScale(2);
    }
}
