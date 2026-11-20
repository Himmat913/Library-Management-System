package com.ucs553.library.model;

public class Staff extends Person {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final Role role;
    private final String passwordHash;

    public Staff(String name, String email, String username, Role role, String passwordHash) {
        super(name, email);
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        this.username = username.trim();
        this.role = role;
        this.passwordHash = passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
