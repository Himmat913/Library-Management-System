package com.ucs553.library.service;

@FunctionalInterface
public interface LoanListener {

    void onLoanEvent(LoanEvent event);
}
