package com.ucs553.library.model;

import com.ucs553.library.exception.ItemUnavailableException;

public interface Borrowable {

    boolean isAvailable();

    void checkOut() throws ItemUnavailableException;

    void checkIn();

    ItemType getType();

    default int getLoanDays() {
        return getType().getLoanDays();
    }
}
