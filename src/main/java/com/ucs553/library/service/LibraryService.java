package com.ucs553.library.service;

import com.ucs553.library.dao.DataAccessException;
import com.ucs553.library.dao.ItemDao;
import com.ucs553.library.dao.LoanDao;
import com.ucs553.library.dao.MemberDao;
import com.ucs553.library.dao.TransactionRunner;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.exception.NotFoundException;
import com.ucs553.library.fine.FinePolicy;
import com.ucs553.library.model.Item;
import com.ucs553.library.model.ItemType;
import com.ucs553.library.model.Loan;
import com.ucs553.library.model.Member;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LibraryService {

    private static final Logger LOG = Logger.getLogger(LibraryService.class.getName());

    private final ItemDao items;
    private final MemberDao members;
    private final LoanDao loans;
    private final FinePolicy finePolicy;
    private final Clock clock;
    private final TransactionRunner transactions;
    private final List<LoanListener> listeners = new CopyOnWriteArrayList<>();

    public LibraryService(ItemDao items, MemberDao members, LoanDao loans, FinePolicy finePolicy, Clock clock,
                          TransactionRunner transactions) {
        this.items = items;
        this.members = members;
        this.loans = loans;
        this.finePolicy = finePolicy;
        this.clock = clock;
        this.transactions = transactions;
    }

    public void addListener(LoanListener listener) {
        listeners.add(listener);
    }

    public void removeListener(LoanListener listener) {
        listeners.remove(listener);
    }

    private record Borrowed(Loan loan, Item item, Member member) {
    }

    private record Returned(Loan loan, Item item) {
    }

    public Loan borrow(int itemId, int memberId) throws LibraryException {
        Borrowed done = transactions.run(() -> {
            Item item = items.findById(itemId).orElseThrow(() -> new NotFoundException("Item", itemId));
            Member member = members.findById(memberId).orElseThrow(() -> new NotFoundException("Member", memberId));

            member.checkCanBorrow(loans.countActiveByMember(memberId));
            item.checkOut();
            items.update(item);

            Loan loan = new Loan(itemId, memberId, today(), item.getType());
            loans.insert(loan);
            return new Borrowed(loan, item, member);
        });
        Loan loan = done.loan();
        LOG.info(done.member().getName() + " borrowed " + done.item().getTitle() + ", due " + loan.getDueOn());
        fire(new LoanEvent(LoanEvent.Type.ISSUED, loan));
        return loan;
    }

    public ReturnResult returnLoan(int loanId) throws LibraryException {
        LocalDate today = today();
        Returned done = transactions.run(() -> {
            Loan loan = loans.findById(loanId).orElseThrow(() -> new NotFoundException("Loan", loanId));
            if (loan.getReturnedOn() != null) {
                throw new LibraryException("This loan was already returned on " + loan.getReturnedOn());
            }
            Item item = items.findById(loan.getItemId()).orElseThrow(() -> new NotFoundException("Item", loan.getItemId()));

            item.checkIn();
            items.update(item);
            loan.markReturned(today);
            loans.update(loan);
            return new Returned(loan, item);
        });
        Loan loan = done.loan();
        BigDecimal fine = finePolicy.calculate(done.item().getType(), loan.overdueDays(today));
        LOG.info("Returned " + done.item().getTitle() + ", fine " + fine);
        fire(new LoanEvent(LoanEvent.Type.RETURNED, loan));
        return new ReturnResult(loan, fine);
    }

    public BigDecimal currentFine(Loan loan) throws LibraryException {
        Item item = items.findById(loan.getItemId()).orElseThrow(() -> new NotFoundException("Item", loan.getItemId()));
        return finePolicy.calculate(item.getType(), loan.overdueDays(today()));
    }

    public BigDecimal fineFor(Loan loan, ItemType type) {
        return finePolicy.calculate(type, loan.overdueDays(today()));
    }

    public List<LoanDetail> loanDetails() throws DataAccessException {
        Map<Integer, Item> itemById = items.findAll().stream().collect(Collectors.toMap(Item::getId, Function.identity()));
        Map<Integer, Member> memberById = members.findAll().stream().collect(Collectors.toMap(Member::getId, Function.identity()));
        LocalDate today = today();
        List<LoanDetail> details = new ArrayList<>();
        for (Loan loan : loans.findAll()) {
            Item item = itemById.get(loan.getItemId());
            Member member = memberById.get(loan.getMemberId());
            details.add(new LoanDetail(loan,
                    item == null ? "(removed)" : item.getTitle(),
                    member == null ? "(removed)" : member.getName(),
                    loan.statusOn(today),
                    item == null ? BigDecimal.ZERO : fineFor(loan, item.getType())));
        }
        return details;
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
