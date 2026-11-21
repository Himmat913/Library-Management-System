package com.ucs553.library.service;

import com.ucs553.library.model.Loan;

import java.math.BigDecimal;

public record ReturnResult(Loan loan, BigDecimal fine, String nextInQueue) {

    public ReturnResult(Loan loan, BigDecimal fine) {
        this(loan, fine, null);
    }

    public boolean hasFine() {
        return fine.signum() > 0;
    }

    public boolean hasNextInQueue() {
        return nextInQueue != null;
    }
}
