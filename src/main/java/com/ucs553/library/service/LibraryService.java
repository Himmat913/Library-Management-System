package com.ucs553.library.service;

import com.ucs553.library.dao.AuditDao;
import com.ucs553.library.dao.DataAccessException;
import com.ucs553.library.dao.FineDao;
import com.ucs553.library.dao.ItemDao;
import com.ucs553.library.dao.LoanDao;
import com.ucs553.library.dao.MemberDao;
import com.ucs553.library.dao.NotificationDao;
import com.ucs553.library.dao.Repositories;
import com.ucs553.library.dao.ReservationDao;
import com.ucs553.library.dao.TransactionRunner;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.exception.NotFoundException;
import com.ucs553.library.fine.FinePolicy;
import com.ucs553.library.model.AuditAction;
import com.ucs553.library.model.AuditEntry;
import com.ucs553.library.model.Fine;
import com.ucs553.library.model.Item;
import com.ucs553.library.model.ItemType;
import com.ucs553.library.model.Loan;
import com.ucs553.library.model.LoanStatus;
import com.ucs553.library.model.Member;
import com.ucs553.library.model.Notification;
import com.ucs553.library.model.Reservation;
import com.ucs553.library.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LibraryService {

    private static final Logger LOG = Logger.getLogger(LibraryService.class.getName());

    private final ItemDao items;
    private final MemberDao members;
    private final LoanDao loans;
    private final ReservationDao reservations;
    private final FineDao fines;
    private final AuditDao audit;
    private final NotificationDao notifications;
    private final FinePolicy finePolicy;
    private final Clock clock;
    private final TransactionRunner transactions;
    private final Supplier<String> actor;
    private final LibraryRules rules;
    private final List<LoanListener> listeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    public LibraryService(Repositories repos, FinePolicy finePolicy, Clock clock, TransactionRunner transactions,
                          Supplier<String> actor, LibraryRules rules) {
        this.items = repos.items();
        this.members = repos.members();
        this.loans = repos.loans();
        this.reservations = repos.reservations();
        this.fines = repos.fines();
        this.audit = repos.audit();
        this.notifications = repos.notifications();
        this.finePolicy = finePolicy;
        this.clock = clock;
        this.transactions = transactions;
        this.actor = actor;
        this.rules = rules;
    }

    public void addListener(LoanListener listener) {
        listeners.add(listener);
    }

    public void removeListener(LoanListener listener) {
        listeners.remove(listener);
    }

    // called after any change to loans, reservations, fines or accounts so open screens can reload
    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    private record Borrowed(Loan loan, Item item, Member member) {
    }

    private record Returned(Loan loan, Item item, BigDecimal fine, String nextInQueue) {
    }

    public Loan borrow(int itemId, int memberId) throws LibraryException {
        Borrowed done = transactions.run(() -> {
            expireLapsedHolds();
            Item item = items.findById(itemId).orElseThrow(() -> new NotFoundException("Item", itemId));
            Member member = members.findById(memberId).orElseThrow(() -> new NotFoundException("Member", memberId));

            checkFinesAllow(member);
            member.checkCanBorrow(loans.countActiveByMember(memberId));

            Optional<Reservation> own = reservations.findOpenFor(itemId, memberId);
            boolean holdsCopy = own.isPresent() && own.get().getStatus() == ReservationStatus.READY;
            if (!holdsCopy) {
                item.checkOut();
                items.update(item);
            }
            if (own.isPresent()) {
                Reservation reservation = own.get();
                reservation.fulfil();
                reservations.update(reservation);
                journal(AuditAction.RESERVATION_FULFILLED, member.getName() + " collected the reserved " + item.getTitle());
            }

            Loan loan = new Loan(itemId, memberId, today(), item.getType());
            loans.insert(loan);
            journal(AuditAction.ISSUED, item.getTitle() + " to " + member.getName() + ", due " + loan.getDueOn());
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
            expireLapsedHolds();
            Loan loan = loans.findById(loanId).orElseThrow(() -> new NotFoundException("Loan", loanId));
            if (loan.getReturnedOn() != null) {
                throw new LibraryException("This loan was already returned on " + loan.getReturnedOn());
            }
            Item item = items.findById(loan.getItemId()).orElseThrow(() -> new NotFoundException("Item", loan.getItemId()));
            Member member = members.findById(loan.getMemberId()).orElse(null);
            String who = member == null ? "member " + loan.getMemberId() : member.getName();

            loan.markReturned(today);
            loans.update(loan);
            journal(AuditAction.RETURNED, item.getTitle() + " from " + who);

            BigDecimal fine = finePolicy.calculate(item.getType(), loan.overdueDays(today));
            if (fine.signum() > 0) {
                fines.insert(new Fine(loan.getId(), loan.getMemberId(), fine, today));
                journal(AuditAction.FINED, who + " fined ₹" + fine + " for " + loan.overdueDays(today)
                        + " days late on " + item.getTitle());
            }

            String next = null;
            Optional<Reservation> promoted = releaseCopy(item);
            if (promoted.isPresent()) {
                next = members.findById(promoted.get().getMemberId()).map(Member::getName).orElse(null);
            }
            return new Returned(loan, item, fine, next);
        });
        LOG.info("Returned " + done.item().getTitle() + ", fine " + done.fine());
        fire(new LoanEvent(LoanEvent.Type.RETURNED, done.loan()));
        return new ReturnResult(done.loan(), done.fine(), done.nextInQueue());
    }

    public Loan renew(int loanId) throws LibraryException {
        Loan renewed = transactions.run(() -> {
            Loan loan = loans.findById(loanId).orElseThrow(() -> new NotFoundException("Loan", loanId));
            if (loan.getReturnedOn() != null) {
                throw new LibraryException("This loan was already returned");
            }
            if (loan.statusOn(today()) == LoanStatus.OVERDUE) {
                throw new LibraryException("An overdue loan has to be returned, it cannot be renewed");
            }
            if (loan.getRenewCount() >= rules.maxRenewals()) {
                throw new LibraryException("This loan has already been renewed the maximum number of times");
            }
            Item item = items.findById(loan.getItemId()).orElseThrow(() -> new NotFoundException("Item", loan.getItemId()));
            if (!reservations.findWaitingByItem(item.getId()).isEmpty()) {
                throw new LibraryException("Someone is waiting for " + item.getTitle() + ", so it cannot be renewed");
            }
            loan.renew(item.getType().getLoanDays());
            loans.update(loan);
            journal(AuditAction.RENEWED, item.getTitle() + " renewed, now due " + loan.getDueOn());
            return loan;
        });
        fire(new LoanEvent(LoanEvent.Type.RENEWED, renewed));
        return renewed;
    }

    public Reservation reserve(int itemId, int memberId) throws LibraryException {
        Reservation created = transactions.run(() -> {
            expireLapsedHolds();
            Item item = items.findById(itemId).orElseThrow(() -> new NotFoundException("Item", itemId));
            Member member = members.findById(memberId).orElseThrow(() -> new NotFoundException("Member", memberId));
            if (item.isAvailable()) {
                throw new LibraryException("A copy of " + item.getTitle() + " is on the shelf, borrow it instead of reserving");
            }
            if (loans.findActiveByMember(memberId).stream().anyMatch(l -> l.getItemId() == itemId)) {
                throw new LibraryException(member.getName() + " already has " + item.getTitle() + " on loan");
            }
            if (reservations.findOpenFor(itemId, memberId).isPresent()) {
                throw new LibraryException(member.getName() + " is already in the queue for " + item.getTitle());
            }
            Reservation reservation = new Reservation(itemId, memberId, today());
            reservations.insert(reservation);
            journal(AuditAction.RESERVED, member.getName() + " joined the queue for " + item.getTitle());
            return reservation;
        });
        changed();
        return created;
    }

    public void cancelReservation(int reservationId) throws LibraryException {
        transactions.run(() -> {
            Reservation reservation = reservations.findById(reservationId)
                    .orElseThrow(() -> new NotFoundException("Reservation", reservationId));
            if (!reservation.getStatus().isOpen()) {
                throw new LibraryException("This reservation is already " + reservation.getStatus().name().toLowerCase());
            }
            boolean heldACopy = reservation.getStatus() == ReservationStatus.READY;
            reservation.cancel();
            reservations.update(reservation);

            Item item = items.findById(reservation.getItemId())
                    .orElseThrow(() -> new NotFoundException("Item", reservation.getItemId()));
            String who = members.findById(reservation.getMemberId()).map(Member::getName).orElse("A member");
            journal(AuditAction.RESERVATION_CANCELLED, who + " no longer waits for " + item.getTitle());
            if (heldACopy) {
                releaseCopy(item);
            }
            return Boolean.TRUE;
        });
        changed();
    }

    // moves held copies on when the member did not come in time
    public void expireHolds() throws LibraryException {
        boolean any = transactions.run(this::expireLapsedHolds);
        if (any) {
            changed();
        }
    }

    public void collectFine(int fineId) throws LibraryException {
        transactions.run(() -> {
            Fine fine = fines.findById(fineId).orElseThrow(() -> new NotFoundException("Fine", fineId));
            if (fine.isPaid()) {
                throw new LibraryException("This fine was already paid on " + fine.getPaidOn());
            }
            fine.markPaid(today());
            fines.update(fine);
            String who = members.findById(fine.getMemberId()).map(Member::getName).orElse("member " + fine.getMemberId());
            journal(AuditAction.FINE_PAID, who + " paid ₹" + fine.getAmount());
            return Boolean.TRUE;
        });
        changed();
    }

    public void recordAccountEvent(AuditAction action, String details) throws DataAccessException {
        journal(action, details);
        changed();
    }

    private boolean expireLapsedHolds() throws LibraryException {
        boolean any = false;
        for (Reservation reservation : reservations.findLapsedHolds(today())) {
            reservation.expire();
            reservations.update(reservation);
            any = true;
            Optional<Item> item = items.findById(reservation.getItemId());
            String title = item.isPresent() ? item.get().getTitle() : "item " + reservation.getItemId();
            String who = members.findById(reservation.getMemberId()).map(Member::getName).orElse("A member");
            journal(AuditAction.RESERVATION_EXPIRED, who + " did not collect " + title + " in time");
            tell(reservation.getMemberId(), "Your hold on " + title + " has expired.");
            if (item.isPresent()) {
                releaseCopy(item.get());
            }
        }
        return any;
    }

    // a returned or released copy goes to the next person waiting, or back on the shelf
    private Optional<Reservation> releaseCopy(Item item) throws LibraryException {
        Queue<Reservation> queue = new LinkedList<>(reservations.findWaitingByItem(item.getId()));
        Reservation next = queue.poll();
        if (next == null) {
            item.checkIn();
            items.update(item);
            return Optional.empty();
        }
        next.markReady(today(), rules.holdDays());
        reservations.update(next);
        String who = members.findById(next.getMemberId()).map(Member::getName).orElse("A member");
        tell(next.getMemberId(), item.getTitle() + " is ready for you. Collect it by " + next.getHoldUntil() + ".");
        journal(AuditAction.RESERVATION_READY, who + " can collect " + item.getTitle() + " until " + next.getHoldUntil());
        return Optional.of(next);
    }

    private void checkFinesAllow(Member member) throws LibraryException {
        BigDecimal limit = rules.fineBlockLimit();
        if (limit.signum() > 0) {
            BigDecimal owed = unpaidTotal(member.getId());
            if (owed.compareTo(limit) >= 0) {
                throw new LibraryException(member.getName() + " owes ₹" + owed
                        + " in unpaid fines. Collect the payment before issuing more items");
            }
        }
    }

    private void journal(AuditAction action, String details) throws DataAccessException {
        audit.insert(new AuditEntry(LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS), actor.get(), action, details));
    }

    private void tell(int memberId, String message) throws DataAccessException {
        notifications.insert(new Notification(memberId, message, LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS)));
    }

    public BigDecimal currentFine(Loan loan) throws LibraryException {
        Item item = items.findById(loan.getItemId()).orElseThrow(() -> new NotFoundException("Item", loan.getItemId()));
        return finePolicy.calculate(item.getType(), loan.overdueDays(today()));
    }

    public BigDecimal fineFor(Loan loan, ItemType type) {
        return finePolicy.calculate(type, loan.overdueDays(today()));
    }

    public List<LoanDetail> loanDetails() throws DataAccessException {
        return detailsOf(loans.findAll());
    }

    public List<LoanDetail> loanDetailsOf(int memberId) throws DataAccessException {
        return detailsOf(loans.findAll().stream().filter(l -> l.getMemberId() == memberId).toList());
    }

    private List<LoanDetail> detailsOf(List<Loan> source) throws DataAccessException {
        Map<Integer, Item> itemById = items.findAll().stream().collect(Collectors.toMap(Item::getId, Function.identity()));
        Map<Integer, Member> memberById = members.findAll().stream().collect(Collectors.toMap(Member::getId, Function.identity()));
        LocalDate today = today();
        List<LoanDetail> details = new ArrayList<>();
        for (Loan loan : source) {
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

    public List<ReservationDetail> reservationDetails() throws DataAccessException {
        return reservationDetailsFor(reservations.findOpen());
    }

    public List<ReservationDetail> reservationDetailsOf(int memberId) throws DataAccessException {
        return reservationDetailsFor(reservations.findByMember(memberId));
    }

    private List<ReservationDetail> reservationDetailsFor(List<Reservation> source) throws DataAccessException {
        Map<Integer, Item> itemById = items.findAll().stream().collect(Collectors.toMap(Item::getId, Function.identity()));
        Map<Integer, Member> memberById = members.findAll().stream().collect(Collectors.toMap(Member::getId, Function.identity()));
        Map<Integer, Queue<Reservation>> queues = new HashMap<>();
        List<ReservationDetail> details = new ArrayList<>();
        for (Reservation r : source) {
            Item item = itemById.get(r.getItemId());
            Member member = memberById.get(r.getMemberId());
            int position = 0;
            if (r.getStatus() == ReservationStatus.WAITING) {
                Queue<Reservation> queue = queues.get(r.getItemId());
                if (queue == null) {
                    queue = new LinkedList<>(reservations.findWaitingByItem(r.getItemId()));
                    queues.put(r.getItemId(), queue);
                }
                position = positionIn(queue, r.getId());
            }
            details.add(new ReservationDetail(r, item == null ? "(removed)" : item.getTitle(),
                    member == null ? "(removed)" : member.getName(), position));
        }
        return details;
    }

    private int positionIn(Queue<Reservation> queue, int reservationId) {
        int place = 1;
        for (Reservation waiting : queue) {
            if (waiting.getId() == reservationId) {
                return place;
            }
            place++;
        }
        return 0;
    }

    public int waitingCount(int itemId) throws DataAccessException {
        return reservations.findWaitingByItem(itemId).size();
    }

    public List<FineDetail> fineDetails(boolean unpaidOnly) throws DataAccessException {
        return fineDetailsFor(unpaidOnly ? fines.findUnpaid() : fines.findAll());
    }

    public List<FineDetail> fineDetailsOf(int memberId) throws DataAccessException {
        return fineDetailsFor(fines.findByMember(memberId));
    }

    private List<FineDetail> fineDetailsFor(List<Fine> source) throws DataAccessException {
        Map<Integer, Loan> loanById = loans.findAll().stream().collect(Collectors.toMap(Loan::getId, Function.identity()));
        Map<Integer, Item> itemById = items.findAll().stream().collect(Collectors.toMap(Item::getId, Function.identity()));
        Map<Integer, Member> memberById = members.findAll().stream().collect(Collectors.toMap(Member::getId, Function.identity()));
        List<FineDetail> details = new ArrayList<>();
        for (Fine fine : source) {
            Loan loan = loanById.get(fine.getLoanId());
            Item item = loan == null ? null : itemById.get(loan.getItemId());
            Member member = memberById.get(fine.getMemberId());
            details.add(new FineDetail(fine, member == null ? "(removed)" : member.getName(),
                    item == null ? "(removed)" : item.getTitle()));
        }
        return details;
    }

    public BigDecimal unpaidTotal(int memberId) throws DataAccessException {
        return fines.findByMember(memberId).stream().filter(f -> !f.isPaid())
                .map(Fine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<AuditEntry> recentActivity(int limit) throws DataAccessException {
        return audit.findRecent(limit);
    }

    public List<Notification> notificationsOf(int memberId) throws DataAccessException {
        return notifications.findByMember(memberId);
    }

    public int unreadCount(int memberId) throws DataAccessException {
        return notifications.countUnread(memberId);
    }

    public void markNotificationsRead(int memberId) throws DataAccessException {
        notifications.markAllRead(memberId);
    }

    public List<PopularItem> mostBorrowed(int limit) throws DataAccessException {
        Map<Integer, Long> counts = loans.findAll().stream()
                .collect(Collectors.groupingBy(Loan::getItemId, Collectors.counting()));
        Map<Integer, Item> itemById = items.findAll().stream().collect(Collectors.toMap(Item::getId, Function.identity()));
        List<PopularItem> ranked = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : counts.entrySet()) {
            Item item = itemById.get(entry.getKey());
            if (item != null) {
                ranked.add(new PopularItem(item.getTitle(), item.getAuthor(), entry.getValue()));
            }
        }
        ranked.sort(Comparator.comparingLong(PopularItem::loans).reversed().thenComparing(PopularItem::title));
        return ranked.size() > limit ? new ArrayList<>(ranked.subList(0, limit)) : ranked;
    }

    public DashboardStats stats() throws DataAccessException {
        LocalDate today = today();
        List<Loan> active = loans.findActive();
        int overdue = (int) active.stream().filter(l -> l.statusOn(today) == LoanStatus.OVERDUE).count();
        BigDecimal unpaid = fines.findUnpaid().stream().map(Fine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardStats(items.findAll().size(), members.findAll().size(), active.size(), overdue,
                reservations.findOpen().size(), unpaid);
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

    public List<String> categories() throws DataAccessException {
        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Item item : items.findAll()) {
            names.add(item.getCategory());
        }
        return new ArrayList<>(names);
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
        changed();
    }

    private void changed() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "A change listener failed", e);
            }
        }
    }
}
