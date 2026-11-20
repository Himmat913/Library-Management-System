package com.ucs553.library.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    @Test
    void buildsTheRightSubclass() {
        Item book = ItemFactory.create(ItemType.BOOK, "A", "B", 2020, 1, Map.of("isbn", "123", "pages", "300"));
        Item magazine = ItemFactory.create(ItemType.MAGAZINE, "A", "B", 2020, 1, Map.of("issue_number", "4"));
        Item digital = ItemFactory.create(ItemType.DIGITAL, "A", "B", 2020, 1, Map.of("format", "epub", "size_mb", "2.5"));
        assertEquals(300, ((Book) book).getPages());
        assertEquals(4, ((Magazine) magazine).getIssueNumber());
        assertEquals("EPUB", ((DigitalResource) digital).getFormat());
    }

    @Test
    void missingExtrasFallBackToDefaults() {
        Book book = (Book) ItemFactory.create(ItemType.BOOK, "A", "B", 2020, 1, Map.of());
        assertEquals(0, book.getPages());
        assertEquals("", book.getIsbn());
    }

    @Test
    void badNumbersAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemFactory.create(ItemType.BOOK, "A", "B", 2020, 1, Map.of("pages", "many")));
    }
}
