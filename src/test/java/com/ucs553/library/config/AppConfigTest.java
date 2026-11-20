package com.ucs553.library.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @TempDir
    Path dir;

    @Test
    void readsValuesFromFile() throws Exception {
        Path file = dir.resolve("app.properties");
        Files.writeString(file, "member.borrowLimit=8\nlog.level=FINE\n");
        AppConfig config = AppConfig.load(file);
        assertTrue(config.isFileFound());
        assertEquals(8, config.getInt("member.borrowLimit"));
        assertEquals("FINE", config.getString("log.level"));
    }

    @Test
    void missingFileFallsBackToDefaults() throws Exception {
        AppConfig config = AppConfig.load(dir.resolve("nothing.properties"));
        assertFalse(config.isFileFound());
        assertEquals(5, config.getInt("member.borrowLimit"));
        assertEquals("jdbc:sqlite:data/library.db", config.getString("db.url"));
    }

    @Test
    void keysNotInFileKeepTheirDefaults() throws Exception {
        Path file = dir.resolve("app.properties");
        Files.writeString(file, "log.level=WARNING\n");
        assertEquals("logs", AppConfig.load(file).getString("log.dir"));
    }

    @Test
    void badNumberIsReported() throws Exception {
        Path file = dir.resolve("app.properties");
        Files.writeString(file, "member.borrowLimit=many\n");
        AppConfig config = AppConfig.load(file);
        assertThrows(ConfigException.class, () -> config.getInt("member.borrowLimit"));
    }
}
