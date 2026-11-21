package com.ucs553.library.dao;

import com.ucs553.library.exception.LibraryException;
import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class Database implements ConnectionSource, TransactionRunner {

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
                size_mb REAL NOT NULL DEFAULT 0,
                category TEXT NOT NULL DEFAULT 'General'
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
                returned_on TEXT,
                renew_count INTEGER NOT NULL DEFAULT 0
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
            """
            CREATE TABLE IF NOT EXISTS reservations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                item_id INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE,
                member_id INTEGER NOT NULL REFERENCES members(id) ON DELETE CASCADE,
                reserved_on TEXT NOT NULL,
                status TEXT NOT NULL CHECK (status IN ('WAITING', 'READY', 'FULFILLED', 'CANCELLED', 'EXPIRED')),
                hold_until TEXT
            )""",
            """
            CREATE TABLE IF NOT EXISTS fines (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                loan_id INTEGER NOT NULL REFERENCES loans(id),
                member_id INTEGER NOT NULL REFERENCES members(id),
                amount TEXT NOT NULL,
                created_on TEXT NOT NULL,
                paid_on TEXT
            )""",
            """
            CREATE TABLE IF NOT EXISTS audit_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                at TEXT NOT NULL,
                actor TEXT NOT NULL,
                action TEXT NOT NULL,
                details TEXT NOT NULL
            )""",
            """
            CREATE TABLE IF NOT EXISTS notifications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                member_id INTEGER NOT NULL REFERENCES members(id) ON DELETE CASCADE,
                message TEXT NOT NULL,
                created_at TEXT NOT NULL,
                is_read INTEGER NOT NULL DEFAULT 0
            )""",
            """
            CREATE TABLE IF NOT EXISTS member_accounts (
                member_id INTEGER PRIMARY KEY REFERENCES members(id) ON DELETE CASCADE,
                password_hash TEXT NOT NULL,
                must_change INTEGER NOT NULL DEFAULT 0
            )""",
            "CREATE INDEX IF NOT EXISTS idx_reservations_item ON reservations(item_id, status)",
            "CREATE INDEX IF NOT EXISTS idx_reservations_member ON reservations(member_id)",
            "CREATE INDEX IF NOT EXISTS idx_fines_member ON fines(member_id)",
            "CREATE INDEX IF NOT EXISTS idx_notifications_member ON notifications(member_id)",
            "CREATE INDEX IF NOT EXISTS idx_loans_member ON loans(member_id)",
            "CREATE INDEX IF NOT EXISTS idx_loans_item ON loans(item_id)");

    private final String url;
    private final SQLiteConfig config = new SQLiteConfig();
    private final ThreadLocal<Connection> current = new ThreadLocal<>();

    public Database(String url) throws DataAccessException {
        this.url = url;
        config.enforceForeignKeys(true);
        createParentFolder();
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection inTransaction = current.get();
        if (inTransaction != null) {
            return inTransaction;
        }
        return DriverManager.getConnection(url, config.toProperties());
    }

    @Override
    public <T> T run(TransactionWork<T> work) throws LibraryException {
        if (current.get() != null) {
            return work.execute();
        }
        try (Connection real = DriverManager.getConnection(url, config.toProperties())) {
            real.setAutoCommit(false);
            current.set(keepOpen(real));
            try {
                T result = work.execute();
                real.commit();
                return result;
            } catch (LibraryException | RuntimeException e) {
                rollback(real, e);
                throw e;
            } finally {
                current.remove();
            }
        } catch (SQLException e) {
            throw new DataAccessException("The transaction could not be completed", e);
        }
    }

    private static void rollback(Connection connection, Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException e) {
            cause.addSuppressed(e);
        }
    }

    // DAOs close their connection after every call; inside a transaction that close must be ignored
    private static Connection keepOpen(Connection real) {
        return (Connection) Proxy.newProxyInstance(Database.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("close")) {
                        return null;
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    public void initSchema() throws DataAccessException {
        run(CREATE);
        addMissingColumns();
    }

    // databases created before reservations existed lack these two columns
    private void addMissingColumns() throws DataAccessException {
        try (Connection c = getConnection()) {
            addColumnIfMissing(c, "items", "category", "TEXT NOT NULL DEFAULT 'General'");
            addColumnIfMissing(c, "loans", "renew_count", "INTEGER NOT NULL DEFAULT 0");
        } catch (SQLException e) {
            throw new DataAccessException("Could not update the database structure", e);
        }
    }

    private void addColumnIfMissing(Connection c, String table, String column, String definition) throws SQLException {
        boolean found = false;
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    found = true;
                }
            }
        }
        if (!found) {
            try (Statement st = c.createStatement()) {
                st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            }
        }
    }

    public void resetSchema() throws DataAccessException {
        run(List.of("DROP TABLE IF EXISTS notifications", "DROP TABLE IF EXISTS member_accounts",
                "DROP TABLE IF EXISTS audit_log", "DROP TABLE IF EXISTS fines", "DROP TABLE IF EXISTS reservations",
                "DROP TABLE IF EXISTS staff", "DROP TABLE IF EXISTS loans", "DROP TABLE IF EXISTS members",
                "DROP TABLE IF EXISTS items"));
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
