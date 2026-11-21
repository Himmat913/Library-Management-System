package com.ucs553.library.service;

import com.ucs553.library.dao.Database;
import com.ucs553.library.dao.Repositories;
import com.ucs553.library.fine.PerDayFinePolicy;

import java.time.Clock;

public final class TestServices {

    private TestServices() {
    }

    public static LibraryService create(Database db, Clock clock) {
        return create(Repositories.jdbc(db), db, clock);
    }

    public static LibraryService create(Repositories repos, Database db, Clock clock) {
        return new LibraryService(repos, new PerDayFinePolicy(), clock, db, () -> "tester", LibraryRules.defaults());
    }
}
