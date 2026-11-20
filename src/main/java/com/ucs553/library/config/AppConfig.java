package com.ucs553.library.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class AppConfig {

    private final Properties props;
    private final boolean fileFound;

    private AppConfig(Properties props, boolean fileFound) {
        this.props = props;
        this.fileFound = fileFound;
    }

    public static AppConfig load(Path file) throws ConfigException {
        Properties p = new Properties(defaultValues());
        if (!Files.exists(file)) {
            return new AppConfig(p, false);
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(reader);
        } catch (IOException | IllegalArgumentException e) {
            throw new ConfigException("Could not read config file " + file, e);
        }
        return new AppConfig(p, true);
    }

    public static AppConfig defaults() {
        return new AppConfig(new Properties(defaultValues()), false);
    }

    private static Properties defaultValues() {
        Properties d = new Properties();
        d.setProperty("app.name", "Library Management System");
        d.setProperty("db.url", "jdbc:sqlite:data/library.db");
        d.setProperty("log.level", "INFO");
        d.setProperty("log.dir", "logs");
        d.setProperty("member.borrowLimit", "5");
        d.setProperty("backup.dir", "data/backups");
        d.setProperty("export.dir", "data/exports");
        d.setProperty("settings.file", "data/settings.ser");
        return d;
    }

    public String getString(String key) {
        return props.getProperty(key).trim();
    }

    public int getInt(String key) throws ConfigException {
        String value = getString(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ConfigException("Setting " + key + " must be a number, found \"" + value + "\"", e);
        }
    }

    public boolean isFileFound() {
        return fileFound;
    }
}
