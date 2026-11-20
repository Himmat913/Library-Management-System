package com.ucs553.library.dao;

import com.ucs553.library.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LoanDaoTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 1);

    @TempDir
    Path dir;

    private LoanDao loans;
    private int itemId;
    private int memberId;

    @BeforeEach
    void setUp() throws Exception {
        Database db = TestDb.create(dir);
        loans = new JdbcLoanDao(db);
        Item item = new Book.Builder().title("A").author("B").year(2020).copies(2).build();
        new JdbcItemDao(db).insert(item);
        Member member = new Member("Himmat", "himmat@example.com");
        new JdbcMemberDao(db).insert(member);
        itemId = item.getId();
        memberId = member.getId();
    }

    @Test
    void loanRoundTripsWithNullReturnDate() throws Exception {
        Loan loan = new Loan(itemId, memberId, DAY, ItemType.BOOK);
        loans.insert(loan);
        Loan loaded = loans.findById(loan.getId()).orElseThrow();
        assertEquals(DAY.plusDays(14), loaded.getDueOn());
        assertNull(loaded.getReturnedOn());
    }

    @Test
    void returnedLoansLeaveTheActiveList() throws Exception {
        Loan loan = new Loan(itemId, memberId, DAY, ItemType.BOOK);
        loans.insert(loan);
        assertEquals(1, loans.countActiveByMember(memberId));
        loan.markReturned(DAY.plusDays(3));
        loans.update(loan);
        assertEquals(0, loans.countActiveByMember(memberId));
        assertTrue(loans.findActive().isEmpty());
        assertEquals(DAY.plusDays(3), loans.findById(loan.getId()).orElseThrow().getReturnedOn());
    }

    @Test
    void overdueQueryUsesTheDueDate() throws Exception {
        loans.insert(new Loan(itemId, memberId, DAY, ItemType.BOOK));
        assertTrue(loans.findOverdue(DAY.plusDays(14)).isEmpty());
        assertEquals(1, loans.findOverdue(DAY.plusDays(15)).size());
    }

    @Test
    void unknownItemOrMemberIsRejected() {
        assertThrows(DataAccessException.class,
                () -> loans.insert(new Loan(999, memberId, DAY, ItemType.BOOK)));
        assertThrows(DataAccessException.class,
                () -> loans.insert(new Loan(itemId, 999, DAY, ItemType.BOOK)));
    }
}
