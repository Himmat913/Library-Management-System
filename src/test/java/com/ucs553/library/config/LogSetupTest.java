package com.ucs553.library.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogSetupTest {

    @TempDir
    Path dir;

    @AfterEach
    void closeHandlers() {
        LogSetup.shutdown();
    }

    @Test
    void parsesLevelsAndFallsBackToInfo() {
        assertEquals(Level.WARNING, LogSetup.parseLevel("warning"));
        assertEquals(Level.INFO, LogSetup.parseLevel("loud"));
    }

    @Test
    void messagesEndUpInLogFile() throws Exception {
        LogSetup.init(Level.INFO, dir);
        Logger.getLogger("com.ucs553.library.test").info("hello log");
        LogSetup.shutdown();
        Path file = dir.resolve("library-0.log");
        assertTrue(Files.exists(file));
        assertTrue(Files.readString(file).contains("hello log"));
    }
}
