package com.ucs553.library;

import com.ucs553.library.model.Member;
import com.ucs553.library.model.Staff;

public final class Session {

    private static Staff current;
    private static Member currentMember;

    private Session() {
    }

    public static synchronized void set(Staff staff) {
        current = staff;
        currentMember = null;
    }

    public static synchronized void setMember(Member member) {
        currentMember = member;
        current = null;
    }

    public static synchronized void clear() {
        current = null;
        currentMember = null;
    }

    public static synchronized Staff current() {
        return current;
    }

    public static synchronized Member currentMember() {
        return currentMember;
    }

    public static synchronized boolean isAdmin() {
        return current != null && current.isAdmin();
    }

    // who the activity log should name for whatever happens right now
    public static synchronized String actorName() {
        if (current != null) {
            return current.getUsername();
        }
        if (currentMember != null) {
            return currentMember.getEmail();
        }
        return "system";
    }
}
