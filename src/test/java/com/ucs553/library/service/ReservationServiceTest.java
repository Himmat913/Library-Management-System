package com.ucs553.library.service;

import com.ucs553.library.dao.Database;
import com.ucs553.library.exception.ItemUnavailableException;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.AuditAction;
import com.ucs553.library.model.AuditEntry;
import com.ucs553.library.model.Book;
import com.ucs553.library.model.Loan;
import com.ucs553.library.model.Member;
import com.ucs553.library.model.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceTest {

    @TempDir
    Path dir;

    private MutableClock clock;
    private LibraryService service;
    private Book book;
    private Book other;
    private Member alice;
    private Member bob;
    private Member carol;

    @BeforeEach
    void setUp() throws Exception {
        Database db = new Database("jdbc:sqlite:" + dir.resolve("test.db"));
        db.initSchema();
        clock = new MutableClock(LocalDate.of(2026, 11, 1));
        service = TestServices.create(db, clock);

        book = new Book.Builder().title("Clean Code").author("Robert Martin").year(2008).copies(1).build();
        other = new Book.Builder().title("Effective Java").author("Joshua Bloch").year(2018).copies(1).build();
        service.saveItem(book);
        service.saveItem(other);
        alice = new Member("Alice", "alice@example.com");
        bob = new Member("Bob", "bob@example.com");
        carol = new Member("Carol", "carol@example.com");
        service.saveMember(alice);
        service.saveMember(bob);
        service.saveMember(carol);
    }

    private ReservationStatus statusOf(Member member) throws Exception {
        return service.reservationDetailsOf(member.getId()).get(0).reservation().getStatus();
    }

    private int shelfCopies() throws Exception {
        return service.findItem(book.getId()).orElseThrow().getAvailableCopies();
    }

    @Test
    void reservingIsRefusedWhileACopyIsOnTheShelf() {
        assertThrows(LibraryException.class, () -> service.reserve(book.getId(), bob.getId()));
    }

    @Test
    void waitingListKeepsFirstComeOrder() throws Exception {
        service.borrow(book.getId(), alice.getId());
        service.reserve(book.getId(), bob.getId());
        service.reserve(book.getId(), carol.getId());

        List<ReservationDetail> details = service.reservationDetails();
        assertEquals(2, details.size());
        assertEquals("Bob", details.get(0).memberName());
        assertEquals(1, details.get(0).position());
        assertEquals("Carol", details.get(1).memberName());
        assertEquals(2, details.get(1).position());
        assertEquals(2, service.waitingCount(book.getId()));
    }

    @Test
    void cannotJoinTheQueueTwiceOrWhileHoldingTheItem() throws Exception {
        service.borrow(book.getId(), alice.getId());
        assertThrows(LibraryException.class, () -> service.reserve(book.getId(), alice.getId()));
        service.reserve(book.getId(), bob.getId());
        assertThrows(LibraryException.class, () -> service.reserve(book.getId(), bob.getId()));
    }

    @Test
    void returnHoldsTheCopyForTheFirstInQueue() throws Exception {
        Loan loan = service.borrow(book.getId(), alice.getId());
        service.reserve(book.getId(), bob.getId());
        service.reserve(book.getId(), carol.getId());

        ReturnResult result = service.returnLoan(loan.getId());

        assertEquals("Bob", result.nextInQueue());
        assertEquals(0, shelfCopies());
        assertEquals(ReservationStatus.READY, statusOf(bob));
        assertEquals(1, service.unreadCount(bob.getId()));
        assertEquals(0, service.unreadCount(carol.getId()));
        assertThrows(ItemUnavailableException.class, () -> service.borrow(book.getId(), carol.getId()));

        Loan bobsLoan = service.borrow(book.getId(), bob.getId());
        assertNotNull(bobsLoan);
        assertEquals(ReservationStatus.FULFILLED, statusOf(bob));
        assertEquals(0, shelfCopies());
    }

    @Test
    void returnWithNobodyWaitingPutsTheCopyBackOnTheShelf() throws Exception {
        Loan loan = service.borrow(book.getId(), alice.getId());
        ReturnResult result = service.returnLoan(loan.getId());
        assertFalse(result.hasNextInQueue());
        assertEquals(1, shelfCopies());
    }

    @Test
    void lapsedHoldPassesToTheNextPersonAndFinallyBackToTheShelf() throws Exception {
        Loan loan = service.borrow(book.getId(), alice.getId());
        service.reserve(book.getId(), bob.getId());
        service.reserve(book.getId(), carol.getId());
        service.returnLoan(loan.getId());

        clock.advanceDays(2);
        service.expireHolds();
        assertEquals(ReservationStatus.READY, statusOf(bob));

        clock.advanceDays(2);
        service.expireHolds();
        assertEquals(ReservationStatus.EXPIRED, statusOf(bob));
        assertEquals(ReservationStatus.READY, statusOf(carol));
        assertEquals(2, service.notificationsOf(bob.getId()).size());
        assertEquals(0, shelfCopies());

        clock.advanceDays(4);
        service.expireHolds();
        assertEquals(ReservationStatus.EXPIRED, statusOf(carol));
        assertEquals(1, shelfCopies());
    }

    @Test
    void cancellingAReadyReservationFreesTheCopy() throws Exception {
        Loan loan = service.borrow(book.getId(), alice.getId());
        service.reserve(book.getId(), bob.getId());
        service.returnLoan(loan.getId());

        int reservationId = service.reservationDetailsOf(bob.getId()).get(0).reservation().getId();
        service.cancelReservation(reservationId);

        assertEquals(ReservationStatus.CANCELLED, statusOf(bob));
        assertEquals(1, shelfCopies());
        assertThrows(LibraryException.class, () -> service.cancelReservation(reservationId));
    }

    @Test
    void renewalExtendsTheDueDateOnlyOnce() throws Exception {
        Loan loan = service.borrow(book.getId(), alice.getId());
        assertEquals(LocalDate.of(2026, 11, 15), loan.getDueOn());

        Loan renewed = service.renew(loan.getId());
        assertEquals(LocalDate.of(2026, 11, 29), renewed.getDueOn());
        assertEquals(1, renewed.getRenewCount());
        assertThrows(LibraryException.class, () -> service.renew(loan.getId()));
    }

    @Test
    void renewalIsRefusedWhenSomeoneIsWaiting() throws Exception {
        Loan loan = service.borrow(book.getId(), alice.getId());
        service.reserve(book.getId(), bob.getId());
        assertThrows(LibraryException.class, () -> service.renew(loan.getId()));
    }

    @Test
    void overdueLoanCannotBeRenewed() throws Exception {
        Loan loan = service.borrow(book.getId(), alice.getId());
        clock.advanceDays(20);
        assertThrows(LibraryException.class, () -> service.renew(loan.getId()));
    }

    @Test
    void lateReturnStoresAFineThatCanBeCollected() throws Exception {
        Loan loan = service.borrow(book.getId(), alice.getId());
        clock.advanceDays(17);
        service.returnLoan(loan.getId());

        List<FineDetail> unpaid = service.fineDetails(true);
        assertEquals(1, unpaid.size());
        assertEquals(0, new BigDecimal("15.00").compareTo(unpaid.get(0).fine().getAmount()));
        assertEquals(0, new BigDecimal("15.00").compareTo(service.unpaidTotal(alice.getId())));

        int fineId = unpaid.get(0).fine().getId();
        service.collectFine(fineId);
        assertTrue(service.fineDetails(true).isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(service.unpaidTotal(alice.getId())));
        assertThrows(LibraryException.class, () -> service.collectFine(fineId));
    }

    @Test
    void largeUnpaidFinesBlockNewLoansUntilPaid() throws Exception {
        Loan loan = service.borrow(book.getId(), alice.getId());
        clock.advanceDays(34);
        service.returnLoan(loan.getId());

        LibraryException blocked = assertThrows(LibraryException.class, () -> service.borrow(other.getId(), alice.getId()));
        assertTrue(blocked.getMessage().contains("unpaid"));

        service.collectFine(service.fineDetails(true).get(0).fine().getId());
        assertNotNull(service.borrow(other.getId(), alice.getId()));
    }

    @Test
    void activityLogNamesWhoDidWhat() throws Exception {
        Loan loan = service.borrow(book.getId(), alice.getId());
        clock.advanceDays(17);
        service.returnLoan(loan.getId());

        List<AuditEntry> log = service.recentActivity(10);
        assertEquals(3, log.size());
        assertEquals(AuditAction.FINED, log.get(0).getAction());
        assertEquals(AuditAction.RETURNED, log.get(1).getAction());
        assertEquals(AuditAction.ISSUED, log.get(2).getAction());
        assertEquals("tester", log.get(0).getActor());
        assertTrue(log.get(2).getDetails().contains("Alice"));
    }

    @Test
    void mostBorrowedRanksByNumberOfLoans() throws Exception {
        Loan first = service.borrow(book.getId(), alice.getId());
        service.returnLoan(first.getId());
        Loan second = service.borrow(book.getId(), bob.getId());
        service.returnLoan(second.getId());
        Loan third = service.borrow(other.getId(), carol.getId());
        service.returnLoan(third.getId());

        List<PopularItem> popular = service.mostBorrowed(5);
        assertEquals("Clean Code", popular.get(0).title());
        assertEquals(2, popular.get(0).loans());
        assertEquals(1, popular.get(1).loans());
    }

    @Test
    void changeListenersHearAboutReservations() throws Exception {
        AtomicInteger changes = new AtomicInteger();
        service.addChangeListener(changes::incrementAndGet);
        service.borrow(book.getId(), alice.getId());
        int afterBorrow = changes.get();
        service.reserve(book.getId(), bob.getId());
        assertEquals(afterBorrow + 1, changes.get());
    }

    @Test
    void dashboardCountsEverything() throws Exception {
        service.borrow(book.getId(), alice.getId());
        service.reserve(book.getId(), bob.getId());
        DashboardStats stats = service.stats();
        assertEquals(2, stats.items());
        assertEquals(3, stats.members());
        assertEquals(1, stats.activeLoans());
        assertEquals(0, stats.overdueLoans());
        assertEquals(1, stats.openReservations());
    }
}
