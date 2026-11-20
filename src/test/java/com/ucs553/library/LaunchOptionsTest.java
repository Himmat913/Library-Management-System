package com.ucs553.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LaunchOptionsTest {

    @Test
    void noArgumentsGivesDefaults() {
        LaunchOptions options = LaunchOptions.parse(new String[0]);

        assertEquals(LaunchOptions.DEFAULT_CONFIG, options.configPath());
        assertFalse(options.resetDb());
        assertFalse(options.seedDemo());
    }

    @Test
    void parsesAllOptions() {
        LaunchOptions options = LaunchOptions.parse(
                new String[] {"--config", "other.properties", "--reset-db", "--seed-demo"});

        assertEquals(Path.of("other.properties"), options.configPath());
        assertTrue(options.resetDb());
        assertTrue(options.seedDemo());
    }

    @Test
    void configWithoutPathIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> LaunchOptions.parse(new String[] {"--config"}));
        assertThrows(IllegalArgumentException.class,
                () -> LaunchOptions.parse(new String[] {"--config", "--reset-db"}));
    }

    @Test
    void unknownArgumentIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> LaunchOptions.parse(new String[] {"--nope"}));
    }
}
