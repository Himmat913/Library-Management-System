package com.ucs553.library.config;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class LogSetup {

    private static final String ROOT_PACKAGE = "com.ucs553.library";

    private LogSetup() {
    }

    public static void init(Level level, Path dir) throws IOException {
        Files.createDirectories(dir);
        Logger root = Logger.getLogger(ROOT_PACKAGE);
        shutdown();
        root.setUseParentHandlers(false);
        root.setLevel(level);

        FileHandler file = new FileHandler(dir.resolve("library-%g.log").toString(), 512 * 1024, 3, true);
        file.setFormatter(new LineFormatter());
        file.setLevel(Level.ALL);
        root.addHandler(file);

        ConsoleHandler console = new ConsoleHandler();
        console.setFormatter(new LineFormatter());
        console.setLevel(level);
        root.addHandler(console);
    }

    public static Level parseLevel(String text) {
        try {
            return Level.parse(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Level.INFO;
        }
    }

    public static void shutdown() {
        Logger root = Logger.getLogger(ROOT_PACKAGE);
        for (Handler h : root.getHandlers()) {
            h.close();
            root.removeHandler(h);
        }
    }

    private static class LineFormatter extends SimpleFormatter {
        @Override
        public String format(LogRecord r) {
            String name = r.getLoggerName() == null ? "" : r.getLoggerName().substring(r.getLoggerName().lastIndexOf('.') + 1);
            String line = String.format("%1$tF %1$tT %2$-7s %3$s - %4$s%n",
                    new Date(r.getMillis()), r.getLevel(), name, formatMessage(r));
            if (r.getThrown() != null) {
                StringWriter sw = new StringWriter();
                r.getThrown().printStackTrace(new PrintWriter(sw));
                line += sw;
            }
            return line;
        }
    }
}
