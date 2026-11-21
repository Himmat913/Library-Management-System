package com.ucs553.library.service;

import com.ucs553.library.dao.DataAccessException;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.Item;
import com.ucs553.library.model.Loan;
import com.ucs553.library.model.Member;
import com.ucs553.library.model.Notification;
import com.ucs553.library.model.Reservation;

import java.math.BigDecimal;
import java.util.List;

/**
 * What a signed-in member is allowed to do. It wraps the normal service for one member and checks
 * that every loan or reservation they touch really is theirs, so the screens cannot be tricked
 * into acting on somebody else's records.
 */
public class MemberPortal {

    private final LibraryService service;
    private final Member member;

    public MemberPortal(LibraryService service, Member member) {
        this.service = service;
        this.member = member;
    }

    public Member member() {
        return member;
    }

    public List<Item> catalogue(String text, String category) throws DataAccessException {
        String query = text == null ? "" : text.trim();
        List<Item> found = query.isEmpty() ? service.allItems() : service.searchItems(query);
        if (category == null || category.isBlank()) {
            return found;
        }
        return found.stream().filter(i -> i.getCategory().equalsIgnoreCase(category)).toList();
    }

    public List<String> categories() throws DataAccessException {
        return service.categories();
    }

    public int waitingCount(int itemId) throws DataAccessException {
        return service.waitingCount(itemId);
    }

    public Reservation reserve(int itemId) throws LibraryException {
        return service.reserve(itemId, member.getId());
    }

    public void cancelReservation(int reservationId) throws LibraryException {
        boolean mine = service.reservationDetailsOf(member.getId()).stream()
                .anyMatch(d -> d.reservation().getId() == reservationId);
        if (!mine) {
            throw new LibraryException("That reservation does not belong to you");
        }
        service.cancelReservation(reservationId);
    }

    public Loan renew(int loanId) throws LibraryException {
        boolean mine = service.activeLoansOf(member.getId()).stream().anyMatch(l -> l.getId() == loanId);
        if (!mine) {
            throw new LibraryException("That loan does not belong to you");
        }
        return service.renew(loanId);
    }

    public List<LoanDetail> loans() throws DataAccessException {
        return service.loanDetailsOf(member.getId());
    }

    public List<ReservationDetail> reservations() throws DataAccessException {
        return service.reservationDetailsOf(member.getId());
    }

    public List<FineDetail> fines() throws DataAccessException {
        return service.fineDetailsOf(member.getId());
    }

    public BigDecimal unpaidTotal() throws DataAccessException {
        return service.unpaidTotal(member.getId());
    }

    public List<Notification> notifications() throws DataAccessException {
        return service.notificationsOf(member.getId());
    }

    public int unreadCount() throws DataAccessException {
        return service.unreadCount(member.getId());
    }

    public void markNotificationsRead() throws DataAccessException {
        service.markNotificationsRead(member.getId());
    }

    public java.time.LocalDate today() {
        return service.today();
    }
}
