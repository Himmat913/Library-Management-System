package com.ucs553.library;

import java.nio.file.Path;

public record LaunchOptions(Path configPath, boolean resetDb, boolean seedDemo) {

    public static final Path DEFAULT_CONFIG = Path.of("config", "app.properties");

    public static LaunchOptions parse(String[] args) {
        Path config = DEFAULT_CONFIG;
        boolean resetDb = false;
        boolean seedDemo = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--config" -> {
                    if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                        throw new IllegalArgumentException("--config needs a file path");
                    }
                    config = Path.of(args[++i]);
                }
                case "--reset-db" -> resetDb = true;
                case "--seed-demo" -> seedDemo = true;
                default -> throw new IllegalArgumentException("unknown argument: " + args[i]);
            }
        }
        return new LaunchOptions(config, resetDb, seedDemo);
    }
}
