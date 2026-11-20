package com.ucs553.library.dao;

import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class Database implements ConnectionSource {

    private static final String PREFIX = "jdbc:sqlite:";

    private static final List<String> CREATE = List.of(
            """
            CREATE TABLE IF NOT EXISTS items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL CHECK (type IN ('BOOK', 'MAGAZINE', 'DIGITAL')),
                title TEXT NOT NULL,
                author TEXT NOT NULL,
                year INTEGER NOT NULL,
                total_copies INTEGER NOT NULL CHECK (total_copies >= 1),
                available_copies INTEGER NOT NULL CHECK (available_copies >= 0),
                isbn TEXT NOT NULL DEFAULT '',
                pages INTEGER NOT NULL DEFAULT 0,
                issue_number INTEGER NOT NULL DEFAULT 0,
                publisher TEXT NOT NULL DEFAULT '',
                format TEXT NOT NULL DEFAULT '',
                size_mb REAL NOT NULL DEFAULT 0
            )""",
            """
            CREATE TABLE IF NOT EXISTS members (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                joined_on TEXT NOT NULL,
                borrow_limit INTEGER NOT NULL CHECK (borrow_limit >= 1)
            )""",
            """
            CREATE TABLE IF NOT EXISTS loans (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                item_id INTEGER NOT NULL REFERENCES items(id),
                member_id INTEGER NOT NULL REFERENCES members(id),
                issued_on TEXT NOT NULL,
                due_on TEXT NOT NULL,
                returned_on TEXT
            )""",
            """
            CREATE TABLE IF NOT EXISTS staff (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                username TEXT NOT NULL UNIQUE COLLATE NOCASE,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL CHECK (role IN ('ADMIN', 'LIBRARIAN'))
            )""",
            "CREATE INDEX IF NOT EXISTS idx_loans_member ON loans(member_id)",
            "CREATE INDEX IF NOT EXISTS idx_loans_item ON loans(item_id)");

    private final String url;
    private final SQLiteConfig config = new SQLiteConfig();

    public Database(String url) throws DataAccessException {
        this.url = url;
        config.enforceForeignKeys(true);
        createParentFolder();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, config.toProperties());
    }

    public void initSchema() throws DataAccessException {
        run(CREATE);
    }

    public void resetSchema() throws DataAccessException {
        run(List.of("DROP TABLE IF EXISTS staff", "DROP TABLE IF EXISTS loans", "DROP TABLE IF EXISTS members", "DROP TABLE IF EXISTS items"));
        initSchema();
    }

    public void backupTo(Path target) throws DataAccessException {
        try {
            Path parent = target.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("VACUUM INTO ?")) {
                ps.setString(1, target.toAbsolutePath().toString());
                ps.execute();
            }
        } catch (IOException | SQLException e) {
            throw new DataAccessException("Could not back up the database to " + target, e);
        }
    }

    private void run(List<String> statements) throws DataAccessException {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            for (String sql : statements) {
                st.execute(sql);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not set up the database", e);
        }
    }

    private void createParentFolder() throws DataAccessException {
        if (!url.startsWith(PREFIX)) {
            return;
        }
        String location = url.substring(PREFIX.length());
        if (location.isBlank() || location.startsWith(":") || location.startsWith("file:")) {
            return;
        }
        try {
            Path parent = Path.of(location).toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (InvalidPathException ignored) {
            // not a plain file path, let the driver deal with it
        } catch (IOException e) {
            throw new DataAccessException("Could not create the database folder", e);
        }
    }
}
