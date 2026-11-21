package com.ucs553.library.dao;

import com.ucs553.library.model.Item;
import com.ucs553.library.model.Loan;
import com.ucs553.library.model.Member;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseMigrationTest {

    @TempDir
    Path dir;

    @Test
    void anOlderDatabaseGainsTheNewColumnsWithoutLosingData() throws Exception {
        String url = "jdbc:sqlite:" + dir.resolve("old.db");
        try (Connection c = DriverManager.getConnection(url); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE items (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, "
                    + "title TEXT NOT NULL, author TEXT NOT NULL, year INTEGER NOT NULL, total_copies INTEGER NOT NULL, "
                    + "available_copies INTEGER NOT NULL, isbn TEXT NOT NULL DEFAULT '', pages INTEGER NOT NULL DEFAULT 0, "
                    + "issue_number INTEGER NOT NULL DEFAULT 0, publisher TEXT NOT NULL DEFAULT '', "
                    + "format TEXT NOT NULL DEFAULT '', size_mb REAL NOT NULL DEFAULT 0)");
            st.execute("INSERT INTO items (type, title, author, year, total_copies, available_copies) "
                    + "VALUES ('BOOK', 'Old Book', 'Someone', 2000, 1, 1)");
            st.execute("CREATE TABLE loans (id INTEGER PRIMARY KEY AUTOINCREMENT, item_id INTEGER NOT NULL, "
                    + "member_id INTEGER NOT NULL, issued_on TEXT NOT NULL, due_on TEXT NOT NULL, returned_on TEXT)");
        }

        Database db = new Database(url);
        db.initSchema();
        db.initSchema();

        List<Item> items = new JdbcItemDao(db).findAll();
        assertEquals(1, items.size());
        assertEquals("Old Book", items.get(0).getTitle());
        assertEquals(Item.DEFAULT_CATEGORY, items.get(0).getCategory());

        JdbcMemberDao members = new JdbcMemberDao(db);
        Member member = new Member("Someone", "someone@example.com");
        members.insert(member);
        JdbcLoanDao loans = new JdbcLoanDao(db);
        loans.insert(new Loan(items.get(0).getId(), member.getId(), LocalDate.of(2026, 11, 1), items.get(0).getType()));
        assertEquals(0, loans.findAll().get(0).getRenewCount());
    }
}
