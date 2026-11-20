package com.ucs553.library.model;

import com.ucs553.library.exception.BorrowLimitExceededException;

import java.time.LocalDate;

public class Member extends Person implements Comparable<Member> {

    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_BORROW_LIMIT = 5;

    private final LocalDate joinedOn;
    private final int borrowLimit;

    public Member(String name, String email, LocalDate joinedOn, int borrowLimit) {
        super(name, email);
        if (borrowLimit < 1) {
            throw new IllegalArgumentException("Borrow limit must be at least 1");
        }
        this.joinedOn = joinedOn;
        this.borrowLimit = borrowLimit;
    }

    public Member(String name, String email) {
        this(name, email, LocalDate.now(), DEFAULT_BORROW_LIMIT);
    }

    public LocalDate getJoinedOn() {
        return joinedOn;
    }

    public int getBorrowLimit() {
        return borrowLimit;
    }

    public void checkCanBorrow(int activeLoans) throws BorrowLimitExceededException {
        if (activeLoans >= borrowLimit) {
            throw new BorrowLimitExceededException(getName(), borrowLimit);
        }
    }

    @Override
    public int compareTo(Member other) {
        return getName().compareToIgnoreCase(other.getName());
    }
}
