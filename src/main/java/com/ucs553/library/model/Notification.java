package com.ucs553.library.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private final int memberId;
    private final String message;
    private final LocalDateTime createdAt;
    private boolean read;

    public Notification(int memberId, String message, LocalDateTime createdAt) {
        this(0, memberId, message, createdAt, false);
    }

    public Notification(int id, int memberId, String message, LocalDateTime createdAt, boolean read) {
        this.id = id;
        this.memberId = memberId;
        this.message = message;
        this.createdAt = createdAt;
        this.read = read;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMemberId() {
        return memberId;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return read;
    }
}
