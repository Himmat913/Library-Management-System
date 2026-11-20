package com.ucs553.library;

import com.ucs553.library.model.Staff;
import com.ucs553.library.ui.LoginView;
import com.ucs553.library.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.util.logging.Logger;

public class LibraryApp extends Application {

    private static final Logger LOG = Logger.getLogger(LibraryApp.class.getName());

    private AppContext context;
    private String name;
    private Scene scene;

    @Override
    public void start(Stage stage) {
        context = AppContext.get();
        name = context.config().getString("app.name");

        scene = new Scene(new Region(), 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        showLogin();

        stage.setTitle(name);
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    private void showLogin() {
        Session.clear();
        scene.setRoot(new LoginView(name, context.auth(), this::showMain));
    }

    private void showMain(Staff user) {
        Session.set(user);
        scene.setRoot(new MainView(name, context.service(), context.auth(), user, this::showLogin));
    }

    @Override
    public void stop() {
        LOG.info("Application closed");
    }
}
