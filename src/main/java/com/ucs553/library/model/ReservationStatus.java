package com.ucs553.library.model;

public enum ReservationStatus {
    WAITING, READY, FULFILLED, CANCELLED, EXPIRED;

    public boolean isOpen() {
        return this == WAITING || this == READY;
    }
}
