package com.ucs553.library.dao;

import com.ucs553.library.exception.LibraryException;

public class DataAccessException extends LibraryException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
