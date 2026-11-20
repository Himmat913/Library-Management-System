package com.ucs553.library.ui;

import com.ucs553.library.service.LibraryService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class MainView extends BorderPane {

    private final ToggleGroup group = new ToggleGroup();
    private final VBox sidebar = new VBox(4);
    private final StringProperty status = new SimpleStringProperty("");

    public MainView(String appName, LibraryService service) {
        Label title = new Label(appName);
        title.getStyleClass().add("app-title");
        title.setWrapText(true);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(16, 10, 16, 10));
        sidebar.setPrefWidth(190);
        sidebar.getChildren().add(title);
        setLeft(sidebar);

        Label statusLabel = new Label();
        statusLabel.textProperty().bind(status);
        HBox statusBar = new HBox(statusLabel);
        statusBar.getStyleClass().add("status-bar");
        setBottom(statusBar);

        group.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) {
                old.setSelected(true);
            }
        });

        addPage("Items", new ItemsView(service, status));
        addPage("Members", new MembersView(service, status));
    }

    private <V extends Region & Refreshable> void addPage(String name, V view) {
        ToggleButton button = new ToggleButton(name);
        button.setToggleGroup(group);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("nav-button");
        button.selectedProperty().addListener((obs, was, selected) -> {
            if (selected) {
                view.refresh();
                setCenter(view);
            }
        });
        sidebar.getChildren().add(button);
        if (group.getSelectedToggle() == null) {
            button.setSelected(true);
        }
    }
}
