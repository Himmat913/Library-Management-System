package com.ucs553.library.service;

import com.ucs553.library.dao.Database;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.Book;
import com.ucs553.library.model.Item;
import com.ucs553.library.model.Loan;
import com.ucs553.library.model.Member;
import com.ucs553.library.model.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemberPortalTest {

    @TempDir
    Path dir;

    private MutableClock clock;
    private LibraryService service;
    private Book scarce;
    private Book plenty;
    private Member alice;
    private Member bob;
    private MemberPortal forAlice;
    private MemberPortal forBob;

    @BeforeEach
    void setUp() throws Exception {
        Database db = new Database("jdbc:sqlite:" + dir.resolve("test.db"));
        db.initSchema();
        clock = new MutableClock(LocalDate.of(2026, 11, 1));
        service = TestServices.create(db, clock);

        scarce = new Book.Builder().title("Clean Code").author("Robert Martin").year(2008).copies(1).build();
        scarce.setCategory("Programming");
        plenty = new Book.Builder().title("Dune").author("Frank Herbert").year(1965).copies(3).build();
        plenty.setCategory("Fiction");
        service.saveItem(scarce);
        service.saveItem(plenty);
        alice = new Member("Alice", "alice@example.com");
        bob = new Member("Bob", "bob@example.com");
        service.saveMember(alice);
        service.saveMember(bob);
        forAlice = new MemberPortal(service, alice);
        forBob = new MemberPortal(service, bob);
    }

    @Test
    void catalogueSearchesAndFiltersByCategory() throws Exception {
        assertEquals(2, forAlice.catalogue("", null).size());
        assertEquals(1, forAlice.catalogue("", "fiction").size());
        assertEquals("Dune", forAlice.catalogue("", "Fiction").get(0).getTitle());
        assertEquals(1, forAlice.catalogue("clean", null).size());
        assertTrue(forAlice.catalogue("clean", "Fiction").isEmpty());
        assertTrue(forAlice.categories().contains("Programming"));
    }

    @Test
    void reservingIsForTheSignedInMemberOnly() throws Exception {
        service.borrow(scarce.getId(), alice.getId());
        forBob.reserve(scarce.getId());

        assertEquals(1, forBob.reservations().size());
        assertTrue(forAlice.reservations().isEmpty());
        assertEquals(1, forAlice.waitingCount(scarce.getId()));
    }

    @Test
    void aMemberCannotCancelSomeoneElsesReservation() throws Exception {
        service.borrow(scarce.getId(), alice.getId());
        forBob.reserve(scarce.getId());
        int bobsReservation = forBob.reservations().get(0).reservation().getId();

        assertThrows(LibraryException.class, () -> forAlice.cancelReservation(bobsReservation));
        assertEquals(ReservationStatus.WAITING, forBob.reservations().get(0).reservation().getStatus());

        forBob.cancelReservation(bobsReservation);
        assertEquals(ReservationStatus.CANCELLED, forBob.reservations().get(0).reservation().getStatus());
    }

    @Test
    void aMemberCannotRenewSomeoneElsesLoan() throws Exception {
        Loan loan = service.borrow(plenty.getId(), alice.getId());

        assertThrows(LibraryException.class, () -> forBob.renew(loan.getId()));
        LocalDate before = loan.getDueOn();
        Loan renewed = forAlice.renew(loan.getId());
        assertTrue(renewed.getDueOn().isAfter(before));
        assertEquals(1, forAlice.loans().get(0).loan().getRenewCount());
    }

    @Test
    void readyNoticeAppearsAndIsMarkedRead() throws Exception {
        Loan loan = service.borrow(scarce.getId(), alice.getId());
        forBob.reserve(scarce.getId());
        service.returnLoan(loan.getId());

        assertEquals(1, forBob.unreadCount());
        assertEquals(1, forBob.notifications().size());
        assertEquals(0, forAlice.unreadCount());

        forBob.markNotificationsRead();
        assertEquals(0, forBob.unreadCount());
        assertEquals(1, forBob.notifications().size());
    }

    @Test
    void finesAreListedForTheOwnerWithTheUnpaidTotal() throws Exception {
        Loan loan = service.borrow(plenty.getId(), alice.getId());
        clock.advanceDays(30);
        service.returnLoan(loan.getId());

        assertEquals(1, forAlice.fines().size());
        assertTrue(forAlice.unpaidTotal().signum() > 0);
        assertTrue(forBob.fines().isEmpty());
        assertEquals(0, forBob.unpaidTotal().signum());

        service.collectFine(forAlice.fines().get(0).fine().getId());
        assertEquals(0, forAlice.unpaidTotal().signum());
    }

    @Test
    void reservingAnItemOnTheShelfIsRefused() {
        assertThrows(LibraryException.class, () -> forBob.reserve(plenty.getId()));
    }
}
