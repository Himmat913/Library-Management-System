package com.ucs553.library.service;

import java.math.BigDecimal;

public record DashboardStats(int items, int members, int activeLoans, int overdueLoans, int openReservations,
                             BigDecimal unpaidFines) {
}
