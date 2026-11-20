package com.ucs553.library.service;

import com.ucs553.library.dao.*;
import com.ucs553.library.exception.BorrowLimitExceededException;
import com.ucs553.library.exception.ItemUnavailableException;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.exception.NotFoundException;
import com.ucs553.library.fine.PerDayFinePolicy;
import com.ucs553.library.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LibraryServiceTest {

    @TempDir
    Path dir;

    private Database db;
    private MutableClock clock;
    private LibraryService service;
    private Book book;
    private Member member;

    @BeforeEach
    void setUp() throws Exception {
        db = new Database("jdbc:sqlite:" + dir.resolve("test.db"));
        db.initSchema();
        clock = new MutableClock(LocalDate.of(2026, 11, 1));
        service = new LibraryService(new JdbcItemDao(db), new JdbcMemberDao(db), new JdbcLoanDao(db),
                new PerDayFinePolicy(), clock, db);

        book = new Book.Builder().title("Effective Java").author("Joshua Bloch").year(2018).copies(1).build();
        service.saveItem(book);
        member = new Member("Himmat", "himmat@example.com", LocalDate.of(2026, 1, 1), 1);
        service.saveMember(member);
    }

    @Test
    void borrowCreatesLoanAndUsesUpACopy() throws Exception {
        Loan loan = service.borrow(book.getId(), member.getId());
        assertEquals(LocalDate.of(2026, 11, 15), loan.getDueOn());
        assertEquals(0, service.findItem(book.getId()).orElseThrow().getAvailableCopies());
        assertEquals(1, service.activeLoanCount(member.getId()));
    }

    @Test
    void activeLoansOfListsOnlyThatMembersOpenLoans() throws Exception {
        Loan loan = service.borrow(book.getId(), member.getId());
        assertEquals(1, service.activeLoansOf(member.getId()).size());
        service.returnLoan(loan.getId());
        assertTrue(service.activeLoansOf(member.getId()).isEmpty());
    }

    @Test
    void borrowFailsWhenNoCopyIsLeft() throws Exception {
        Member other = new Member("Priya", "priya@example.com");
        service.saveMember(other);
        service.borrow(book.getId(), member.getId());
        assertThrows(ItemUnavailableException.class, () -> service.borrow(book.getId(), other.getId()));
    }

    @Test
    void borrowFailsAtMemberLimit() throws Exception {
        Book second = new Book.Builder().title("Clean Code").author("Robert Martin").year(2008).build();
        service.saveItem(second);
        service.borrow(book.getId(), member.getId());
        assertThrows(BorrowLimitExceededException.class, () -> service.borrow(second.getId(), member.getId()));
        assertEquals(1, service.findItem(second.getId()).orElseThrow().getAvailableCopies());
    }

    @Test
    void unknownIdsAreReported() {
        assertThrows(NotFoundException.class, () -> service.borrow(999, member.getId()));
        assertThrows(NotFoundException.class, () -> service.borrow(book.getId(), 999));
        assertThrows(NotFoundException.class, () -> service.returnLoan(999));
    }

    @Test
    void returnOnTimeHasNoFine() throws Exception {
        Loan loan = service.borrow(book.getId(), member.getId());
        clock.advanceDays(10);
        ReturnResult result = service.returnLoan(loan.getId());
        assertFalse(result.hasFine());
        assertEquals(1, service.findItem(book.getId()).orElseThrow().getAvailableCopies());
    }

    @Test
    void lateReturnIsFinedPerDay() throws Exception {
        Loan loan = service.borrow(book.getId(), member.getId());
        clock.advanceDays(17);
        assertEquals(1, service.overdueLoans().size());
        assertEquals(new BigDecimal("15.00"), service.currentFine(loan));
        assertEquals(new BigDecimal("15.00"), service.fineFor(loan, ItemType.BOOK));
        ReturnResult result = service.returnLoan(loan.getId());
        assertEquals(new BigDecimal("15.00"), result.fine());
        assertTrue(service.overdueLoans().isEmpty());
    }

    @Test
    void returningTwiceIsRefused() throws Exception {
        Loan loan = service.borrow(book.getId(), member.getId());
        service.returnLoan(loan.getId());
        assertThrows(LibraryException.class, () -> service.returnLoan(loan.getId()));
    }

    @Test
    void listenersHearAboutLoans() throws Exception {
        List<LoanEvent.Type> seen = new ArrayList<>();
        service.addListener(e -> seen.add(e.type()));
        Loan loan = service.borrow(book.getId(), member.getId());
        service.returnLoan(loan.getId());
        assertEquals(List.of(LoanEvent.Type.ISSUED, LoanEvent.Type.RETURNED), seen);
    }

    @Test
    void aBrokenListenerDoesNotStopTheLoan() throws Exception {
        service.addListener(e -> {
            throw new IllegalStateException("boom");
        });
        assertDoesNotThrow(() -> service.borrow(book.getId(), member.getId()));
    }

    @Test
    void duplicateMemberEmailIsRefused() {
        assertThrows(LibraryException.class, () -> service.saveMember(new Member("Someone", "HIMMAT@example.com")));
    }

    @Test
    void savingAnExistingMemberUpdatesIt() throws Exception {
        Member edited = new Member("Himmat Sidhu", "himmat@example.com", member.getJoinedOn(), 4);
        edited.setId(member.getId());
        service.saveMember(edited);
        assertEquals(4, service.findMember(member.getId()).orElseThrow().getBorrowLimit());
    }

    @Test
    void failedBorrowRollsBackTheCopyCount() throws Exception {
        LibraryService failing = new LibraryService(new JdbcItemDao(db), new JdbcMemberDao(db), new JdbcLoanDao(db) {
            @Override
            public int insert(Loan loan) throws DataAccessException {
                throw new DataAccessException("forced failure");
            }
        }, new PerDayFinePolicy(), clock, db);

        assertThrows(DataAccessException.class, () -> failing.borrow(book.getId(), member.getId()));
        assertEquals(1, service.findItem(book.getId()).orElseThrow().getAvailableCopies());
        assertEquals(0, service.activeLoanCount(member.getId()));
    }

    @Test
    void failedReturnLeavesLoanAndCopyCountUntouched() throws Exception {
        Loan loan = service.borrow(book.getId(), member.getId());
        LibraryService failing = new LibraryService(new JdbcItemDao(db), new JdbcMemberDao(db), new JdbcLoanDao(db) {
            @Override
            public void update(Loan changed) throws DataAccessException {
                throw new DataAccessException("forced failure");
            }
        }, new PerDayFinePolicy(), clock, db);

        assertThrows(DataAccessException.class, () -> failing.returnLoan(loan.getId()));
        assertEquals(0, service.findItem(book.getId()).orElseThrow().getAvailableCopies());
        assertEquals(1, service.activeLoanCount(member.getId()));
    }
}
