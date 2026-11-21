package com.ucs553.library.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SettingsStoreTest {

    @TempDir
    Path dir;

    @Test
    void savedSettingsComeBackUnchanged() {
        SettingsStore store = new SettingsStore(dir.resolve("data").resolve("settings.ser"));
        UserSettings settings = new UserSettings();
        settings.setWindowSize(1280, 800);
        settings.setLastPage("Members");
        settings.setLastUsername("priya");
        store.save(settings);

        UserSettings loaded = store.load();
        assertEquals(1280, loaded.getWindowWidth());
        assertEquals("Members", loaded.getLastPage());
        assertEquals("priya", loaded.getLastUsername());
        assertNotNull(loaded.getSavedAt());
    }

    @Test
    void darkModeChoiceIsRemembered() {
        SettingsStore store = new SettingsStore(dir.resolve("settings.ser"));
        UserSettings settings = new UserSettings();
        assertFalse(settings.isDarkMode());
        settings.setDarkMode(true);
        store.save(settings);
        assertTrue(store.load().isDarkMode());
    }

    @Test
    void transientFieldIsNotSaved() {
        SettingsStore store = new SettingsStore(dir.resolve("settings.ser"));
        UserSettings settings = new UserSettings();
        settings.setLastPage("Items");
        assertTrue(settings.isChanged());
        store.save(settings);
        assertFalse(store.load().isChanged());
    }

    @Test
    void missingFileGivesDefaults() {
        UserSettings loaded = new SettingsStore(dir.resolve("none.ser")).load();
        assertEquals(UserSettings.DEFAULT_WIDTH, loaded.getWindowWidth());
        assertEquals("Dashboard", loaded.getLastPage());
    }

    @Test
    void damagedFileGivesDefaults() throws Exception {
        Path file = dir.resolve("settings.ser");
        Files.write(file, new byte[]{1, 2, 3, 4, 5});
        assertEquals("Dashboard", new SettingsStore(file).load().getLastPage());
    }

    @Test
    void unexpectedClassesAreRefused() throws Exception {
        Path file = dir.resolve("settings.ser");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(new ArrayList<>(List.of("not settings")));
        }
        assertEquals("Dashboard", new SettingsStore(file).load().getLastPage());
    }
}
