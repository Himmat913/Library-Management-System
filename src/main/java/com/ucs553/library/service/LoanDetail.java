package com.ucs553.library.service;

import com.ucs553.library.model.Loan;
import com.ucs553.library.model.LoanStatus;

import java.math.BigDecimal;

public record LoanDetail(Loan loan, String itemTitle, String memberName, LoanStatus status, BigDecimal fine) {
}
