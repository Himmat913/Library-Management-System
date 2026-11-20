package com.ucs553.library.dao;

import com.ucs553.library.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemDaoTest {

    @TempDir
    Path dir;

    private Database db;
    private ItemDao items;

    @BeforeEach
    void setUp() throws Exception {
        db = TestDb.create(dir);
        items = new JdbcItemDao(db);
    }

    private Book book(String title) {
        return new Book.Builder().title(title).author("Herbert Schildt").year(2022)
                .copies(3).isbn("978-1").pages(1200).build();
    }

    @Test
    void insertAssignsIdAndRoundTripsABook() throws Exception {
        Book b = book("Java: The Complete Reference");
        int id = items.insert(b);
        assertTrue(id > 0);
        Item loaded = items.findById(id).orElseThrow();
        assertInstanceOf(Book.class, loaded);
        assertEquals("978-1", ((Book) loaded).getIsbn());
        assertEquals(3, loaded.getAvailableCopies());
    }

    @Test
    void everyItemTypeComesBackAsItsOwnClass() throws Exception {
        items.insert(book("A"));
        items.insert(new Magazine("National Geographic", "Various", 2024, 2, 11, "NatGeo"));
        items.insert(new DigitalResource("Effective Java", "Joshua Bloch", 2018, 5, "pdf", 12.5));
        List<Item> all = items.findAll();
        assertEquals(3, all.size());
        assertInstanceOf(Magazine.class, all.get(1));
        assertEquals("PDF", ((DigitalResource) all.get(2)).getFormat());
    }

    @Test
    void updateStoresNewAvailability() throws Exception {
        Book b = book("A");
        items.insert(b);
        b.checkOut();
        items.update(b);
        assertEquals(2, items.findById(b.getId()).orElseThrow().getAvailableCopies());
    }

    @Test
    void searchMatchesTitleOrAuthorIgnoringCase() throws Exception {
        items.insert(book("Java Basics"));
        items.insert(new Book.Builder().title("Python Basics").author("Guido").year(2020).build());
        assertEquals(1, items.search("PYTHON").size());
        assertEquals(1, items.search("schildt").size());
        assertEquals(2, items.search("basics").size());
    }

    @Test
    void deleteRemovesTheRow() throws Exception {
        Book b = book("A");
        items.insert(b);
        items.delete(b.getId());
        assertTrue(items.findById(b.getId()).isEmpty());
        assertThrows(DataAccessException.class, () -> items.delete(b.getId()));
    }

    @Test
    void itemWithLoansCannotBeDeleted() throws Exception {
        Book b = book("A");
        items.insert(b);
        Member m = new Member("Himmat", "himmat@example.com");
        new JdbcMemberDao(db).insert(m);
        new JdbcLoanDao(db).insert(new Loan(b.getId(), m.getId(), LocalDate.of(2026, 11, 1), ItemType.BOOK));
        assertThrows(DataAccessException.class, () -> items.delete(b.getId()));
    }
}
