package com.ucs553.library.model;

import java.util.Map;

public final class ItemFactory {

    private ItemFactory() {
    }

    // extra holds the type-specific fields: isbn, pages, issue_number, publisher, format, size_mb
    public static Item create(ItemType type, String title, String author, int year, int copies,
                              Map<String, String> extra) {
        Item item = switch (type) {
            case BOOK -> new Book.Builder().title(title).author(author).year(year).copies(copies)
                    .isbn(extra.get("isbn")).pages(whole(extra, "pages")).build();
            case MAGAZINE -> new Magazine(title, author, year, copies,
                    whole(extra, "issue_number"), extra.get("publisher"));
            case DIGITAL -> new DigitalResource(title, author, year, copies,
                    extra.get("format"), decimal(extra, "size_mb"));
        };
        item.setCategory(extra.get("category"));
        return item;
    }

    private static int whole(Map<String, String> extra, String key) {
        String value = extra.get(key);
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a whole number");
        }
    }

    private static double decimal(Map<String, String> extra, String key) {
        String value = extra.get(key);
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a number");
        }
    }
}
