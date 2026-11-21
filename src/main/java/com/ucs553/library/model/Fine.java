package com.ucs553.library.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Fine implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private final int loanId;
    private final int memberId;
    private final BigDecimal amount;
    private final LocalDate createdOn;
    private LocalDate paidOn;

    public Fine(int loanId, int memberId, BigDecimal amount, LocalDate createdOn) {
        this(0, loanId, memberId, amount, createdOn, null);
    }

    public Fine(int id, int loanId, int memberId, BigDecimal amount, LocalDate createdOn, LocalDate paidOn) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("A fine must be more than zero");
        }
        this.id = id;
        this.loanId = loanId;
        this.memberId = memberId;
        this.amount = amount;
        this.createdOn = createdOn;
        this.paidOn = paidOn;
    }

    public void markPaid(LocalDate date) {
        if (paidOn != null) {
            throw new IllegalStateException("This fine is already paid");
        }
        this.paidOn = date;
    }

    public boolean isPaid() {
        return paidOn != null;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLoanId() {
        return loanId;
    }

    public int getMemberId() {
        return memberId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getCreatedOn() {
        return createdOn;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }
}
