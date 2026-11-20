package com.ucs553.library.dao;

import com.ucs553.library.model.Loan;

import java.time.LocalDate;
import java.util.List;

public interface LoanDao extends Dao<Loan> {

    List<Loan> findActive() throws DataAccessException;

    List<Loan> findActiveByMember(int memberId) throws DataAccessException;

    List<Loan> findOverdue(LocalDate today) throws DataAccessException;

    int countActiveByMember(int memberId) throws DataAccessException;
}
