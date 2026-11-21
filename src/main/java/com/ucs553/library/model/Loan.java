package com.ucs553.library.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Loan implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private final int itemId;
    private final int memberId;
    private final LocalDate issuedOn;
    private LocalDate dueOn;
    private LocalDate returnedOn;
    private int renewCount;

    public Loan(int itemId, int memberId, LocalDate issuedOn, ItemType type) {
        this(0, itemId, memberId, issuedOn, issuedOn.plusDays(type.getLoanDays()), null);
    }

    public Loan(int id, int itemId, int memberId, LocalDate issuedOn, LocalDate dueOn, LocalDate returnedOn) {
        this.id = id;
        this.itemId = itemId;
        this.memberId = memberId;
        this.issuedOn = issuedOn;
        this.dueOn = dueOn;
        this.returnedOn = returnedOn;
    }

    public void markReturned(LocalDate date) {
        if (returnedOn != null) {
            throw new IllegalStateException("Loan is already returned");
        }
        this.returnedOn = date;
    }

    public void renew(int extraDays) {
        if (returnedOn != null) {
            throw new IllegalStateException("A returned loan cannot be renewed");
        }
        this.dueOn = dueOn.plusDays(extraDays);
        this.renewCount++;
    }

    public int getRenewCount() {
        return renewCount;
    }

    public void setRenewCount(int renewCount) {
        this.renewCount = renewCount;
    }

    // overdue is worked out from the dates instead of being stored
    public LoanStatus statusOn(LocalDate today) {
        if (returnedOn != null) {
            return LoanStatus.RETURNED;
        }
        return today.isAfter(dueOn) ? LoanStatus.OVERDUE : LoanStatus.ACTIVE;
    }

    public long overdueDays(LocalDate today) {
        LocalDate end = returnedOn != null ? returnedOn : today;
        return Math.max(0, ChronoUnit.DAYS.between(dueOn, end));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getItemId() {
        return itemId;
    }

    public int getMemberId() {
        return memberId;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public LocalDate getDueOn() {
        return dueOn;
    }

    public LocalDate getReturnedOn() {
        return returnedOn;
    }
}
