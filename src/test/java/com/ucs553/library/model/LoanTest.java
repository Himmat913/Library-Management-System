package com.ucs553.library.model;

import com.ucs553.library.exception.BorrowLimitExceededException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LoanTest {

    private final LocalDate issued = LocalDate.of(2026, 11, 1);

    @Test
    void dueDateComesFromItemType() {
        Loan loan = new Loan(1, 1, issued, ItemType.BOOK);
        assertEquals(issued.plusDays(14), loan.getDueOn());
    }

    @Test
    void statusIsDerivedFromDates() {
        Loan loan = new Loan(1, 1, issued, ItemType.BOOK);
        assertEquals(LoanStatus.ACTIVE, loan.statusOn(issued.plusDays(14)));
        assertEquals(LoanStatus.OVERDUE, loan.statusOn(issued.plusDays(15)));
        loan.markReturned(issued.plusDays(20));
        assertEquals(LoanStatus.RETURNED, loan.statusOn(issued.plusDays(30)));
    }

    @Test
    void overdueDaysStopCountingAfterReturn() {
        Loan loan = new Loan(1, 1, issued, ItemType.BOOK);
        loan.markReturned(issued.plusDays(19));
        assertEquals(5, loan.overdueDays(issued.plusDays(60)));
    }

    @Test
    void noOverdueDaysBeforeDueDate() {
        Loan loan = new Loan(1, 1, issued, ItemType.MAGAZINE);
        assertEquals(0, loan.overdueDays(issued.plusDays(3)));
    }

    @Test
    void returningTwiceIsAnError() {
        Loan loan = new Loan(1, 1, issued, ItemType.BOOK);
        loan.markReturned(issued.plusDays(2));
        assertThrows(IllegalStateException.class, () -> loan.markReturned(issued.plusDays(3)));
    }

    @Test
    void memberCannotBorrowPastLimit() {
        Member m = new Member("Himmat", "himmat@example.com", issued, 2);
        assertDoesNotThrow(() -> m.checkCanBorrow(1));
        assertThrows(BorrowLimitExceededException.class, () -> m.checkCanBorrow(2));
    }

    @Test
    void personValidatesEmail() {
        assertThrows(IllegalArgumentException.class, () -> new Member("Himmat", "not-an-email"));
    }
}
