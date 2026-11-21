package com.ucs553.library.service;

import com.ucs553.library.dao.Database;
import com.ucs553.library.dao.JdbcMemberAccountDao;
import com.ucs553.library.dao.JdbcStaffDao;
import com.ucs553.library.dao.Repositories;
import com.ucs553.library.fine.FinePolicy;
import com.ucs553.library.fine.PerDayFinePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DemoDataSeederTest {

    @TempDir
    Path dir;

    @Test
    void seedsAFullLibraryOnceWithLoginsForEveryMember() throws Exception {
        Database db = new Database("jdbc:sqlite:" + dir.resolve("test.db"));
        db.initSchema();
        Clock clock = Clock.systemDefaultZone();
        FinePolicy policy = new PerDayFinePolicy();
        Repositories repos = Repositories.jdbc(db);
        LibraryService service = new LibraryService(repos, policy, clock, db, () -> "system", LibraryRules.defaults());
        AuthService auth = new AuthService(new JdbcStaffDao(db), repos.members(), new JdbcMemberAccountDao(db));
        DemoDataSeeder seeder = new DemoDataSeeder(repos, service, auth, policy, clock);

        assertTrue(seeder.seed());
        assertTrue(repos.items().findAll().size() >= 100);
        assertEquals(60, repos.members().findAll().size());
        assertEquals(60, auth.membersWithAccounts().size());
        assertTrue(repos.loans().findActive().size() >= 40);
        assertFalse(repos.loans().findOverdue(LocalDate.now(clock)).isEmpty());
        assertFalse(repos.reservations().findOpen().isEmpty());
        assertFalse(repos.fines().findUnpaid().isEmpty());
        assertTrue(repos.audit().findRecent(1000).size() > 100);
        assertTrue(auth.loginMember("aarav.mehta@example.com", DemoDataSeeder.DEMO_PASSWORD.toCharArray()).isPresent());

        int itemCount = repos.items().findAll().size();
        assertFalse(seeder.seed());
        assertEquals(itemCount, repos.items().findAll().size());
    }
}
