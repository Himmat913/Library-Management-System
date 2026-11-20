package com.ucs553.library.service;

import com.ucs553.library.dao.DataAccessException;
import com.ucs553.library.dao.ItemDao;
import com.ucs553.library.dao.LoanDao;
import com.ucs553.library.dao.MemberDao;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.exception.NotFoundException;
import com.ucs553.library.fine.FinePolicy;
import com.ucs553.library.model.Item;
import com.ucs553.library.model.Loan;
import com.ucs553.library.model.Member;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LibraryService {

    private static final Logger LOG = Logger.getLogger(LibraryService.class.getName());

    private final ItemDao items;
    private final MemberDao members;
    private final LoanDao loans;
    private final FinePolicy finePolicy;
    private final Clock clock;
    private final List<LoanListener> listeners = new CopyOnWriteArrayList<>();

    public LibraryService(ItemDao items, MemberDao members, LoanDao loans, FinePolicy finePolicy, Clock clock) {
        this.items = items;
        this.members = members;
        this.loans = loans;
        this.finePolicy = finePolicy;
        this.clock = clock;
    }

    public void addListener(LoanListener listener) {
        listeners.add(listener);
    }

    public void removeListener(LoanListener listener) {
        listeners.remove(listener);
    }

    public Loan borrow(int itemId, int memberId) throws LibraryException {
        Item item = items.findById(itemId).orElseThrow(() -> new NotFoundException("Item", itemId));
        Member member = members.findById(memberId).orElseThrow(() -> new NotFoundException("Member", memberId));

        member.checkCanBorrow(loans.countActiveByMember(memberId));
        item.checkOut();
        items.update(item);

        Loan loan = new Loan(itemId, memberId, today(), item.getType());
        try {
            loans.insert(loan);
        } catch (DataAccessException e) {
            item.checkIn();
            items.update(item);
            throw e;
        }
        LOG.info(member.getName() + " borrowed " + item.getTitle() + ", due " + loan.getDueOn());
        fire(new LoanEvent(LoanEvent.Type.ISSUED, loan));
        return loan;
    }

    public ReturnResult returnLoan(int loanId) throws LibraryException {
        Loan loan = loans.findById(loanId).orElseThrow(() -> new NotFoundException("Loan", loanId));
        if (loan.getReturnedOn() != null) {
            throw new LibraryException("This loan was already returned on " + loan.getReturnedOn());
        }
        Item item = items.findById(loan.getItemId()).orElseThrow(() -> new NotFoundException("Item", loan.getItemId()));

        LocalDate today = today();
        item.checkIn();
        items.update(item);
        loan.markReturned(today);
        try {
            loans.update(loan);
        } catch (DataAccessException e) {
            item.checkOut();
            items.update(item);
            throw e;
        }

        BigDecimal fine = finePolicy.calculate(item.getType(), loan.overdueDays(today));
        LOG.info("Returned " + item.getTitle() + ", fine " + fine);
        fire(new LoanEvent(LoanEvent.Type.RETURNED, loan));
        return new ReturnResult(loan, fine);
    }

    public BigDecimal currentFine(Loan loan) throws LibraryException {
        Item item = items.findById(loan.getItemId()).orElseThrow(() -> new NotFoundException("Item", loan.getItemId()));
        return finePolicy.calculate(item.getType(), loan.overdueDays(today()));
    }

    public List<Loan> activeLoans() throws DataAccessException {
        return loans.findActive();
    }

    public List<Loan> activeLoansOf(int memberId) throws DataAccessException {
        return loans.findActiveByMember(memberId);
    }

    public List<Loan> overdueLoans() throws DataAccessException {
        return loans.findOverdue(today());
    }

    public List<Loan> allLoans() throws DataAccessException {
        return loans.findAll();
    }

    public List<Item> allItems() throws DataAccessException {
        return items.findAll();
    }

    public List<Item> searchItems(String text) throws DataAccessException {
        return text == null || text.isBlank() ? items.findAll() : items.search(text);
    }

    public Optional<Item> findItem(int id) throws DataAccessException {
        return items.findById(id);
    }

    public void saveItem(Item item) throws DataAccessException {
        if (item.getId() == 0) {
            items.insert(item);
        } else {
            items.update(item);
        }
    }

    public void deleteItem(int id) throws DataAccessException {
        items.delete(id);
    }

    public List<Member> allMembers() throws DataAccessException {
        return members.findAll();
    }

    public List<Member> searchMembers(String text) throws DataAccessException {
        return text == null || text.isBlank() ? members.findAll() : members.search(text);
    }

    public Optional<Member> findMember(int id) throws DataAccessException {
        return members.findById(id);
    }

    public void saveMember(Member member) throws LibraryException {
        Optional<Member> sameEmail = members.findByEmail(member.getEmail());
        if (sameEmail.isPresent() && sameEmail.get().getId() != member.getId()) {
            throw new LibraryException("A member with the email " + member.getEmail() + " already exists");
        }
        if (member.getId() == 0) {
            members.insert(member);
        } else {
            members.update(member);
        }
    }

    public void deleteMember(int id) throws DataAccessException {
        members.delete(id);
    }

    public int activeLoanCount(int memberId) throws DataAccessException {
        return loans.countActiveByMember(memberId);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    private void fire(LoanEvent event) {
        for (LoanListener listener : listeners) {
            try {
                listener.onLoanEvent(event);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "A loan listener failed", e);
            }
        }
    }
}
