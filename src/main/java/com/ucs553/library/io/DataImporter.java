package com.ucs553.library.io;

import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.Item;
import com.ucs553.library.model.ItemFactory;
import com.ucs553.library.model.ItemType;
import com.ucs553.library.model.Member;
import com.ucs553.library.service.LibraryService;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataImporter {

    private record Row(int line, Map<String, String> values) {
    }

    private final LibraryService service;

    public DataImporter(LibraryService service) {
        this.service = service;
    }

    public ImportResult importItems(Path file) throws IOException, LibraryException {
        List<Row> rows = read(file, "type", "title", "author", "year", "copies");
        Set<Item> known = new HashSet<>(service.allItems());
        List<String> errors = new ArrayList<>();
        int imported = 0;

        for (Row row : rows) {
            Map<String, String> v = row.values();
            try {
                Item item = ItemFactory.create(parseType(v.get("type")), required(v, "title"), required(v, "author"),
                        whole(v, "year"), whole(v, "copies"), v);
                if (!known.add(item)) {
                    throw new IllegalArgumentException("already in the catalogue");
                }
                service.saveItem(item);
                imported++;
            } catch (IllegalArgumentException | LibraryException e) {
                errors.add("Line " + row.line() + ": " + e.getMessage());
            }
        }
        return new ImportResult(imported, errors);
    }

    public ImportResult importMembers(Path file) throws IOException, LibraryException {
        List<Row> rows = read(file, "name", "email");
        List<String> errors = new ArrayList<>();
        int imported = 0;

        for (Row row : rows) {
            Map<String, String> v = row.values();
            try {
                String joined = v.get("joined_on");
                String limit = v.get("borrow_limit");
                Member member = new Member(v.get("name"), v.get("email"),
                        joined == null || joined.isBlank() ? LocalDate.now() : LocalDate.parse(joined.trim()),
                        limit == null || limit.isBlank() ? Member.DEFAULT_BORROW_LIMIT : whole(v, "borrow_limit"));
                service.saveMember(member);
                imported++;
            } catch (IllegalArgumentException | DateTimeException | LibraryException e) {
                errors.add("Line " + row.line() + ": " + e.getMessage());
            }
        }
        return new ImportResult(imported, errors);
    }

    private List<Row> read(Path file, String... requiredColumns) throws IOException, LibraryException {
        List<Row> rows = new ArrayList<>();
        try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String headerLine = in.readLine();
            if (headerLine == null) {
                throw new LibraryException("The file is empty");
            }
            List<String> header = Csv.parse(headerLine.replace("﻿", "")).stream()
                    .map(h -> h.trim().toLowerCase()).toList();
            for (String column : requiredColumns) {
                if (!header.contains(column)) {
                    throw new LibraryException("The file has no \"" + column + "\" column");
                }
            }

            String text;
            int lineNumber = 1;
            while ((text = in.readLine()) != null) {
                lineNumber++;
                if (text.isBlank()) {
                    continue;
                }
                List<String> cells = Csv.parse(text);
                Map<String, String> values = new HashMap<>();
                for (int i = 0; i < header.size(); i++) {
                    values.put(header.get(i), i < cells.size() ? cells.get(i).trim() : "");
                }
                rows.add(new Row(lineNumber, values));
            }
        }
        return rows;
    }

    private static ItemType parseType(String text) {
        try {
            return ItemType.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown type \"" + text + "\" (use BOOK, MAGAZINE or DIGITAL)");
        }
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is missing");
        }
        return value;
    }

    private static int whole(Map<String, String> values, String key) {
        try {
            return Integer.parseInt(required(values, key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a whole number");
        }
    }
}
