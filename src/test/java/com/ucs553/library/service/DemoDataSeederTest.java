package com.ucs553.library.service;

import com.ucs553.library.dao.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.*;

class DemoDataSeederTest {

    @TempDir
    Path dir;

    @Test
    void seedsOnceAndIncludesOverdueLoans() throws Exception {
        Database db = new Database("jdbc:sqlite:" + dir.resolve("test.db"));
        db.initSchema();
        Clock clock = Clock.systemDefaultZone();
        JdbcItemDao items = new JdbcItemDao(db);
        JdbcMemberDao members = new JdbcMemberDao(db);
        JdbcLoanDao loans = new JdbcLoanDao(db);
        DemoDataSeeder seeder = new DemoDataSeeder(items, members, loans, clock);

        assertTrue(seeder.seed());
        assertEquals(9, items.findAll().size());
        assertEquals(4, members.findAll().size());
        assertEquals(6, loans.findActive().size());
        assertEquals(3, loans.findOverdue(java.time.LocalDate.now(clock)).size());

        assertFalse(seeder.seed());
        assertEquals(9, items.findAll().size());
    }
}
