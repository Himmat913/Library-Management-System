package com.ucs553.library.service;

import com.ucs553.library.model.Loan;

import java.math.BigDecimal;

public record ReturnResult(Loan loan, BigDecimal fine) {

    public boolean hasFine() {
        return fine.signum() > 0;
    }
}
