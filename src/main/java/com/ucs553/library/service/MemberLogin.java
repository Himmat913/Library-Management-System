package com.ucs553.library.service;

import com.ucs553.library.model.Member;

public record MemberLogin(Member member, boolean mustChangePassword) {
}
