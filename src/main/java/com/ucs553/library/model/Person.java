package com.ucs553.library.model;

import java.io.Serializable;

public abstract class Person implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private final String name;
    private final String email;

    protected Person(String name, String email) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (email == null || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("A valid email is required");
        }
        this.name = name.trim();
        this.email = email.trim().toLowerCase();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Person p && getClass() == p.getClass() && email.equals(p.email);
    }

    @Override
    public int hashCode() {
        return email.hashCode();
    }

    @Override
    public String toString() {
        return name + " <" + email + ">";
    }
}
