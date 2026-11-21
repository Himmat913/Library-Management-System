package com.ucs553.library.dao;

import com.ucs553.library.model.AuditAction;
import com.ucs553.library.model.AuditEntry;
import com.ucs553.library.model.Book;
import com.ucs553.library.model.Fine;
import com.ucs553.library.model.Item;
import com.ucs553.library.model.Loan;
import com.ucs553.library.model.Member;
import com.ucs553.library.model.Notification;
import com.ucs553.library.model.Reservation;
import com.ucs553.library.model.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReservationDaoTest {

    @TempDir
    Path dir;

    private Repositories repos;
    private Item item;
    private Member first;
    private Member second;

    @BeforeEach
    void setUp() throws Exception {
        Database db = TestDb.create(dir);
        repos = Repositories.jdbc(db);
        item = new Book.Builder().title("Clean Code").author("Robert Martin").year(2008).build();
        repos.items().insert(item);
        first = new Member("First", "first@example.com");
        second = new Member("Second", "second@example.com");
        repos.members().insert(first);
        repos.members().insert(second);
    }

    @Test
    void waitingListComesBackInOrderAndSkipsClosedReservations() throws Exception {
        Reservation a = new Reservation(item.getId(), first.getId(), LocalDate.of(2026, 11, 1));
        Reservation b = new Reservation(item.getId(), second.getId(), LocalDate.of(2026, 11, 2));
        repos.reservations().insert(a);
        repos.reservations().insert(b);
        a.cancel();
        repos.reservations().update(a);

        List<Reservation> waiting = repos.reservations().findWaitingByItem(item.getId());
        assertEquals(1, waiting.size());
        assertEquals(second.getId(), waiting.get(0).getMemberId());
        assertTrue(repos.reservations().findOpenFor(item.getId(), first.getId()).isEmpty());
        assertTrue(repos.reservations().findOpenFor(item.getId(), second.getId()).isPresent());
    }

    @Test
    void lapsedHoldsAreFoundByDate() throws Exception {
        Reservation r = new Reservation(item.getId(), first.getId(), LocalDate.of(2026, 11, 1));
        repos.reservations().insert(r);
        r.markReady(LocalDate.of(2026, 11, 1), 3);
        repos.reservations().update(r);

        assertTrue(repos.reservations().findLapsedHolds(LocalDate.of(2026, 11, 4)).isEmpty());
        assertEquals(1, repos.reservations().findLapsedHolds(LocalDate.of(2026, 11, 5)).size());
        assertEquals(ReservationStatus.READY, repos.reservations().findAll().get(0).getStatus());
    }

    @Test
    void deletingAMemberRemovesTheirReservations() throws Exception {
        repos.reservations().insert(new Reservation(item.getId(), first.getId(), LocalDate.of(2026, 11, 1)));
        repos.members().delete(first.getId());
        assertTrue(repos.reservations().findAll().isEmpty());
    }

    @Test
    void finesKeepExactAmountsAndPaidState() throws Exception {
        Loan loan = new Loan(item.getId(), first.getId(), LocalDate.of(2026, 11, 1), item.getType());
        repos.loans().insert(loan);
        Fine fine = new Fine(loan.getId(), first.getId(), new BigDecimal("15.50"), LocalDate.of(2026, 11, 20));
        repos.fines().insert(fine);

        assertEquals(1, repos.fines().findUnpaid().size());
        fine.markPaid(LocalDate.of(2026, 11, 21));
        repos.fines().update(fine);
        assertTrue(repos.fines().findUnpaid().isEmpty());
        Fine stored = repos.fines().findByMember(first.getId()).get(0);
        assertEquals(new BigDecimal("15.50"), stored.getAmount());
        assertEquals(LocalDate.of(2026, 11, 21), stored.getPaidOn());
    }

    @Test
    void notificationsCountUnreadAndCanBeMarkedRead() throws Exception {
        repos.notifications().insert(new Notification(first.getId(), "Ready", LocalDateTime.of(2026, 11, 1, 10, 0)));
        repos.notifications().insert(new Notification(first.getId(), "Expired", LocalDateTime.of(2026, 11, 2, 10, 0)));
        repos.notifications().insert(new Notification(second.getId(), "Other", LocalDateTime.of(2026, 11, 2, 11, 0)));

        assertEquals(2, repos.notifications().countUnread(first.getId()));
        assertEquals("Expired", repos.notifications().findByMember(first.getId()).get(0).getMessage());
        repos.notifications().markAllRead(first.getId());
        assertEquals(0, repos.notifications().countUnread(first.getId()));
        assertEquals(1, repos.notifications().countUnread(second.getId()));
    }

    @Test
    void auditEntriesComeBackNewestFirst() throws Exception {
        repos.audit().insert(new AuditEntry(LocalDateTime.of(2026, 11, 1, 9, 0), "admin", AuditAction.ISSUED, "one"));
        repos.audit().insert(new AuditEntry(LocalDateTime.of(2026, 11, 1, 10, 0), "admin", AuditAction.RETURNED, "two"));
        List<AuditEntry> recent = repos.audit().findRecent(1);
        assertEquals(1, recent.size());
        assertEquals("two", recent.get(0).getDetails());
        assertEquals(AuditAction.RETURNED, recent.get(0).getAction());
    }
}
