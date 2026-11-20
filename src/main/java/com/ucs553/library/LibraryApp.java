package com.ucs553.library;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class LibraryApp extends Application {

    @Override
    public void start(Stage stage) {
        Label title = new Label("Library Management System");
        title.setStyle("-fx-font-size: 22px;");

        stage.setTitle("Library Management System");
        stage.setScene(new Scene(new BorderPane(title), 1000, 650));
        stage.show();
    }
}
