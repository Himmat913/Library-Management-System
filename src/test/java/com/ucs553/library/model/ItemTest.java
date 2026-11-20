package com.ucs553.library.model;

import com.ucs553.library.exception.ItemUnavailableException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemTest {

    private Book book(String title, int copies) {
        return new Book.Builder().title(title).author("Herbert Schildt").year(2022)
                .copies(copies).isbn("978-1").pages(1200).build();
    }

    @Test
    void checkOutReducesAvailableCopies() throws Exception {
        Book b = book("Java: The Complete Reference", 2);
        b.checkOut();
        assertEquals(1, b.getAvailableCopies());
        assertTrue(b.isAvailable());
    }

    @Test
    void checkOutFailsWhenNoCopiesLeft() throws Exception {
        Book b = book("Java: The Complete Reference", 1);
        b.checkOut();
        assertThrows(ItemUnavailableException.class, b::checkOut);
    }

    @Test
    void checkInNeverExceedsTotal() {
        Book b = book("Java: The Complete Reference", 1);
        b.checkIn();
        assertEquals(1, b.getAvailableCopies());
    }

    @Test
    void itemsSortByTitleIgnoringCase() {
        List<Item> items = new ArrayList<>(List.of(book("zebra", 1), book("Apple", 1), book("mango", 1)));
        Collections.sort(items);
        assertEquals("Apple", items.get(0).getTitle());
        assertEquals("zebra", items.get(2).getTitle());
    }

    @Test
    void subclassesReportTheirType() {
        Item mag = new Magazine("National Geographic", "Various", 2024, 3, 11, "NatGeo");
        Item digital = new DigitalResource("Effective Java", "Joshua Bloch", 2018, 5, "pdf", 12.5);
        assertEquals(ItemType.MAGAZINE, mag.getType());
        assertEquals(21, digital.getLoanDays());
        assertTrue(digital.describe().contains("PDF"));
    }

    @Test
    void invalidDataIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> book(" ", 1));
        assertThrows(IllegalArgumentException.class, () -> book("Title", 0));
    }
}
