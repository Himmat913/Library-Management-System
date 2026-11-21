package com.ucs553.library.dao;

public record Repositories(ItemDao items, MemberDao members, LoanDao loans, ReservationDao reservations,
                           FineDao fines, AuditDao audit, NotificationDao notifications) {

    public static Repositories jdbc(ConnectionSource source) {
        return new Repositories(new JdbcItemDao(source), new JdbcMemberDao(source), new JdbcLoanDao(source),
                new JdbcReservationDao(source), new JdbcFineDao(source), new JdbcAuditDao(source),
                new JdbcNotificationDao(source));
    }

    public Repositories withLoans(LoanDao replacement) {
        return new Repositories(items, members, replacement, reservations, fines, audit, notifications);
    }

    public Repositories withReservations(ReservationDao replacement) {
        return new Repositories(items, members, loans, replacement, fines, audit, notifications);
    }
}
