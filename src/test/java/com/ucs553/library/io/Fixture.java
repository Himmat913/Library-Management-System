package com.ucs553.library.io;

import com.ucs553.library.dao.Database;
import com.ucs553.library.dao.JdbcItemDao;
import com.ucs553.library.dao.JdbcLoanDao;
import com.ucs553.library.dao.JdbcMemberDao;
import com.ucs553.library.fine.PerDayFinePolicy;
import com.ucs553.library.service.LibraryService;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

final class Fixture {

    private final JdbcItemDao items;
    private final JdbcMemberDao members;
    private final JdbcLoanDao loans;

    Fixture(Path dir) throws Exception {
        Database db = new Database("jdbc:sqlite:" + dir.resolve("test.db"));
        db.initSchema();
        items = new JdbcItemDao(db);
        members = new JdbcMemberDao(db);
        loans = new JdbcLoanDao(db);
    }

    LibraryService on(LocalDate day) {
        Clock clock = Clock.fixed(day.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        return new LibraryService(items, members, loans, new PerDayFinePolicy(), clock);
    }
}
