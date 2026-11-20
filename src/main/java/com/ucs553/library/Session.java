package com.ucs553.library;

import com.ucs553.library.model.Staff;

public final class Session {

    private static Staff current;

    private Session() {
    }

    public static synchronized void set(Staff staff) {
        current = staff;
    }

    public static synchronized void clear() {
        current = null;
    }

    public static synchronized Staff current() {
        return current;
    }

    public static synchronized boolean isAdmin() {
        return current != null && current.isAdmin();
    }
}
