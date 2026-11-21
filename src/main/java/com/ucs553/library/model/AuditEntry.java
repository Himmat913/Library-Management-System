package com.ucs553.library.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AuditEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private final LocalDateTime at;
    private final String actor;
    private final AuditAction action;
    private final String details;

    public AuditEntry(LocalDateTime at, String actor, AuditAction action, String details) {
        this(0, at, actor, action, details);
    }

    public AuditEntry(int id, LocalDateTime at, String actor, AuditAction action, String details) {
        this.id = id;
        this.at = at;
        this.actor = actor;
        this.action = action;
        this.details = details;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getAt() {
        return at;
    }

    public String getActor() {
        return actor;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }
}
