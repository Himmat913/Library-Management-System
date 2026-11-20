package com.ucs553.library.service;

import com.ucs553.library.dao.DataAccessException;
import com.ucs553.library.dao.ItemDao;
import com.ucs553.library.dao.LoanDao;
import com.ucs553.library.dao.MemberDao;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.Book;
import com.ucs553.library.model.DigitalResource;
import com.ucs553.library.model.Item;
import com.ucs553.library.model.Loan;
import com.ucs553.library.model.Magazine;
import com.ucs553.library.model.Member;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DemoDataSeeder {

    private static final Logger LOG = Logger.getLogger(DemoDataSeeder.class.getName());

    private final ItemDao items;
    private final MemberDao members;
    private final LoanDao loans;
    private final Clock clock;

    public DemoDataSeeder(ItemDao items, MemberDao members, LoanDao loans, Clock clock) {
        this.items = items;
        this.members = members;
        this.loans = loans;
        this.clock = clock;
    }

    public boolean seed() throws LibraryException {
        if (!items.findAll().isEmpty() || !members.findAll().isEmpty()) {
            LOG.info("Database already has data, demo data not added");
            return false;
        }
        LocalDate today = LocalDate.now(clock);

        List<Item> catalogue = new ArrayList<>();
        catalogue.add(book("Java: The Complete Reference", "Herbert Schildt", 2022, 3, "DEMO-0001", 1248));
        catalogue.add(book("Effective Java", "Joshua Bloch", 2018, 2, "DEMO-0002", 412));
        catalogue.add(book("Head First Java", "Kathy Sierra", 2022, 2, "DEMO-0003", 752));
        catalogue.add(book("Clean Code", "Robert C. Martin", 2008, 2, "DEMO-0004", 464));
        catalogue.add(book("Introduction to Algorithms", "Thomas H. Cormen", 2022, 1, "DEMO-0005", 1312));
        catalogue.add(new Magazine("National Geographic", "Various", 2026, 3, 11, "NatGeo Partners"));
        catalogue.add(new Magazine("IEEE Spectrum", "IEEE", 2026, 2, 10, "IEEE"));
        catalogue.add(new DigitalResource("Think Java", "Allen Downey", 2020, 5, "pdf", 3.8));
        catalogue.add(new DigitalResource("The Java Language Specification", "Oracle", 2025, 10, "pdf", 4.2));
        for (Item item : catalogue) {
            items.insert(item);
        }

        List<Member> people = List.of(
                new Member("Aarav Mehta", "aarav.mehta@example.com"),
                new Member("Priya Sharma", "priya.sharma@example.com"),
                new Member("Rahul Verma", "rahul.verma@example.com"),
                new Member("Simran Kaur", "simran.kaur@example.com", today.minusMonths(6), 2));
        for (Member m : people) {
            members.insert(m);
        }

        lend(catalogue.get(0), people.get(0), today.minusDays(3));
        lend(catalogue.get(1), people.get(0), today.minusDays(20));
        lend(catalogue.get(2), people.get(1), today.minusDays(10));
        lend(catalogue.get(3), people.get(1), today.minusDays(1));
        lend(catalogue.get(5), people.get(2), today.minusDays(12));
        lend(catalogue.get(7), people.get(3), today.minusDays(25));

        LOG.info("Demo data added: " + catalogue.size() + " items, " + people.size() + " members");
        return true;
    }

    private Book book(String title, String author, int year, int copies, String isbn, int pages) {
        return new Book.Builder().title(title).author(author).year(year).copies(copies)
                .isbn(isbn).pages(pages).build();
    }

    private void lend(Item item, Member member, LocalDate issuedOn) throws LibraryException {
        item.checkOut();
        items.update(item);
        loans.insert(new Loan(item.getId(), member.getId(), issuedOn, item.getType()));
    }
}
