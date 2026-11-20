package com.ucs553.library;

import com.ucs553.library.config.AppConfig;
import com.ucs553.library.config.ConfigException;
import com.ucs553.library.config.LogSetup;
import javafx.application.Application;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

public class Launcher {

    // separate from LibraryApp so the app also starts from a plain jar
    public static void main(String[] args) {
        LaunchOptions options = null;
        try {
            options = LaunchOptions.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("usage: [--config <file>] [--reset-db] [--seed-demo]");
            System.exit(2);
        }

        AppConfig config = null;
        try {
            config = AppConfig.load(options.configPath());
            LogSetup.init(LogSetup.parseLevel(config.getString("log.level")), Path.of(config.getString("log.dir")));
        } catch (ConfigException | IOException e) {
            System.err.println("Startup failed: " + e.getMessage());
            System.exit(1);
        }

        Logger log = Logger.getLogger(Launcher.class.getName());
        if (!config.isFileFound()) {
            log.warning("Config file " + options.configPath() + " not found, using built-in defaults");
        }
        log.info("Starting " + config.getString("app.name"));
        Application.launch(LibraryApp.class, args);
    }
}
