package com.ucs553.library.model;

public enum AuditAction {
    ISSUED("Issued"),
    RETURNED("Returned"),
    RENEWED("Renewed"),
    FINED("Fined"),
    FINE_PAID("Fine paid"),
    RESERVED("Reserved"),
    RESERVATION_READY("Reservation ready"),
    RESERVATION_FULFILLED("Reservation fulfilled"),
    RESERVATION_CANCELLED("Reservation cancelled"),
    RESERVATION_EXPIRED("Reservation expired"),
    ACCOUNT_CREATED("Account created"),
    PASSWORD_RESET("Password reset");

    private final String label;

    AuditAction(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
