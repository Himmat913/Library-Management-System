package com.ucs553.library.model;

import java.io.Serializable;
import java.time.LocalDate;

public class Reservation implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private final int itemId;
    private final int memberId;
    private final LocalDate reservedOn;
    private ReservationStatus status;
    private LocalDate holdUntil;

    public Reservation(int itemId, int memberId, LocalDate reservedOn) {
        this(0, itemId, memberId, reservedOn, ReservationStatus.WAITING, null);
    }

    public Reservation(int id, int itemId, int memberId, LocalDate reservedOn,
                       ReservationStatus status, LocalDate holdUntil) {
        this.id = id;
        this.itemId = itemId;
        this.memberId = memberId;
        this.reservedOn = reservedOn;
        this.status = status;
        this.holdUntil = holdUntil;
    }

    public void markReady(LocalDate today, int holdDays) {
        requireStatus(ReservationStatus.WAITING);
        this.status = ReservationStatus.READY;
        this.holdUntil = today.plusDays(holdDays);
    }

    public void fulfil() {
        if (!status.isOpen()) {
            throw new IllegalStateException("Reservation is already " + status);
        }
        this.status = ReservationStatus.FULFILLED;
    }

    public void cancel() {
        if (!status.isOpen()) {
            throw new IllegalStateException("Reservation is already " + status);
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public void expire() {
        requireStatus(ReservationStatus.READY);
        this.status = ReservationStatus.EXPIRED;
    }

    public boolean holdHasLapsed(LocalDate today) {
        return status == ReservationStatus.READY && holdUntil != null && today.isAfter(holdUntil);
    }

    private void requireStatus(ReservationStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Reservation is " + status + ", expected " + expected);
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getItemId() {
        return itemId;
    }

    public int getMemberId() {
        return memberId;
    }

    public LocalDate getReservedOn() {
        return reservedOn;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDate getHoldUntil() {
        return holdUntil;
    }
}
