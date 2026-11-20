package com.ucs553.library.exception;

public class ItemUnavailableException extends LibraryException {

    public ItemUnavailableException(String title) {
        super("No copies of \"" + title + "\" are available right now");
    }
}
