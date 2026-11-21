package com.ucs553.library;

import com.ucs553.library.config.SettingsStore;
import com.ucs553.library.config.UserSettings;
import com.ucs553.library.model.Member;
import com.ucs553.library.model.Staff;
import com.ucs553.library.service.MemberLogin;
import com.ucs553.library.service.MemberPortal;
import com.ucs553.library.ui.LoginView;
import com.ucs553.library.ui.MainView;
import com.ucs553.library.ui.MemberMainView;
import com.ucs553.library.ui.MemberSignIn;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.logging.Logger;

public class LibraryApp extends Application {

    private static final Logger LOG = Logger.getLogger(LibraryApp.class.getName());

    private AppContext context;
    private String name;
    private Scene scene;
    private SettingsStore settingsStore;
    private UserSettings settings;

    @Override
    public void start(Stage stage) {
        context = AppContext.get();
        name = context.config().getString("app.name");
        settingsStore = new SettingsStore(Path.of(context.config().getString("settings.file")));
        settings = settingsStore.load();

        scene = new Scene(new Region(), Math.max(900, settings.getWindowWidth()), Math.max(560, settings.getWindowHeight()));
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
        scene.setRoot(new LoginView(name, context.auth(), settings.getLastUsername(), this::showMain, this::showMember));
    }

    private void showMain(Staff user) {
        Session.set(user);
        settings.setLastUsername(user.getUsername());
        scene.setRoot(new MainView(name, context.service(), context.auth(), user, settings, this::showLogin));
    }

    private void showMember(MemberLogin login) {
        Member member = login.member();
        Session.setMember(member);
        if (login.mustChangePassword() && !MemberSignIn.completeFirstLogin(scene.getWindow(), context.auth(), member)) {
            showLogin();
            return;
        }
        MemberPortal portal = new MemberPortal(context.service(), member);
        scene.setRoot(new MemberMainView(name, portal, context.auth(), this::showLogin));
    }

    @Override
    public void stop() {
        settings.setWindowSize(scene.getWidth(), scene.getHeight());
        settingsStore.save(settings);
        LOG.info("Application closed");
    }
}
