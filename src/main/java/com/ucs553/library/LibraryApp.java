package com.ucs553.library;

import com.ucs553.library.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Logger;

public class LibraryApp extends Application {

    private static final Logger LOG = Logger.getLogger(LibraryApp.class.getName());

    @Override
    public void start(Stage stage) {
        AppContext context = AppContext.get();
        String name = context.config().getString("app.name");

        Scene scene = new Scene(new MainView(name, context.service()), 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle(name);
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        LOG.info("Application closed");
    }
}
