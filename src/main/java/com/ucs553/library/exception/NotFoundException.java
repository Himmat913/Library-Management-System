package com.ucs553.library.exception;

public class NotFoundException extends LibraryException {

    public NotFoundException(String what, int id) {
        super(what + " " + id + " was not found");
    }
}
