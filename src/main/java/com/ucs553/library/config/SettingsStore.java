package com.ucs553.library.config;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SettingsStore {

    private static final Logger LOG = Logger.getLogger(SettingsStore.class.getName());

    // the file is ours, but only the expected classes are accepted when reading it back
    private static final ObjectInputFilter FILTER = ObjectInputFilter.Config.createFilter(
            "com.ucs553.library.config.UserSettings;java.time.*;java.lang.*;!*");

    private final Path file;

    public SettingsStore(Path file) {
        this.file = file;
    }

    public UserSettings load() {
        if (!Files.exists(file)) {
            return new UserSettings();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            in.setObjectInputFilter(FILTER);
            return (UserSettings) in.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            LOG.log(Level.WARNING, "Could not read " + file + ", using default settings", e);
            return new UserSettings();
        }
    }

    public void save(UserSettings settings) {
        settings.stamp();
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(temp))) {
                out.writeObject(settings);
            }
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not save the settings to " + file, e);
        }
    }
}
