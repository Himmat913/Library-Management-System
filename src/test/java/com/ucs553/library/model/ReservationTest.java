package com.ucs553.library.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    private final LocalDate day = LocalDate.of(2026, 11, 1);

    @Test
    void startsWaitingAndBecomesReadyWithAHoldDate() {
        Reservation r = new Reservation(1, 2, day);
        assertEquals(ReservationStatus.WAITING, r.getStatus());
        r.markReady(day, 3);
        assertEquals(ReservationStatus.READY, r.getStatus());
        assertEquals(day.plusDays(3), r.getHoldUntil());
        assertFalse(r.holdHasLapsed(day.plusDays(3)));
        assertTrue(r.holdHasLapsed(day.plusDays(4)));
    }

    @Test
    void closedReservationsCannotChangeAgain() {
        Reservation r = new Reservation(1, 2, day);
        r.cancel();
        assertThrows(IllegalStateException.class, r::cancel);
        assertThrows(IllegalStateException.class, r::fulfil);
        assertThrows(IllegalStateException.class, () -> r.markReady(day, 3));
    }

    @Test
    void onlyAReadyReservationCanExpire() {
        Reservation r = new Reservation(1, 2, day);
        assertThrows(IllegalStateException.class, r::expire);
        r.markReady(day, 3);
        r.expire();
        assertEquals(ReservationStatus.EXPIRED, r.getStatus());
    }

    @Test
    void renewingAnActiveLoanMovesTheDueDate() {
        Loan loan = new Loan(1, 2, day, ItemType.BOOK);
        loan.renew(14);
        assertEquals(day.plusDays(28), loan.getDueOn());
        assertEquals(1, loan.getRenewCount());
        loan.markReturned(day.plusDays(5));
        assertThrows(IllegalStateException.class, () -> loan.renew(14));
    }

    @Test
    void aFineMustBePositiveAndCanBePaidOnce() {
        assertThrows(IllegalArgumentException.class, () -> new Fine(1, 2, java.math.BigDecimal.ZERO, day));
        Fine fine = new Fine(1, 2, java.math.BigDecimal.TEN, day);
        assertFalse(fine.isPaid());
        fine.markPaid(day);
        assertTrue(fine.isPaid());
        assertThrows(IllegalStateException.class, () -> fine.markPaid(day));
    }
}
