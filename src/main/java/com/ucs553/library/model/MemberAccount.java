package com.ucs553.library.model;

public record MemberAccount(int memberId, String passwordHash, boolean mustChange) {
}
