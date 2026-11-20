package com.ucs553.library.service;

import com.ucs553.library.model.Loan;

public record LoanEvent(Type type, Loan loan) {

    public enum Type {
        ISSUED, RETURNED
    }
}
