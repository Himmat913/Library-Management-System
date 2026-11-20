package com.ucs553.library.exception;

public class BorrowLimitExceededException extends LibraryException {

    public BorrowLimitExceededException(String memberName, int limit) {
        super(memberName + " has already reached the limit of " + limit + " items");
    }
}
