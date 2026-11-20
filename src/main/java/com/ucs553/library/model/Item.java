package com.ucs553.library.model;

import com.ucs553.library.exception.ItemUnavailableException;

import java.io.Serializable;
import java.util.Objects;

public abstract class Item implements Borrowable, Comparable<Item>, Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private final String title;
    private final String author;
    private final int year;
    private final int totalCopies;
    private int availableCopies;

    protected Item(String title, String author, int year, int totalCopies) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException("Author is required");
        }
        if (totalCopies < 1) {
            throw new IllegalArgumentException("At least one copy is needed");
        }
        this.title = title.trim();
        this.author = author.trim();
        this.year = year;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
    }

    public abstract String describe();

    @Override
    public boolean isAvailable() {
        return availableCopies > 0;
    }

    @Override
    public void checkOut() throws ItemUnavailableException {
        if (!isAvailable()) {
            throw new ItemUnavailableException(title);
        }
        availableCopies--;
    }

    @Override
    public void checkIn() {
        if (availableCopies < totalCopies) {
            availableCopies++;
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public int getYear() {
        return year;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    // used when loading a saved item back from the database
    public void restoreAvailableCopies(int copies) {
        if (copies < 0 || copies > totalCopies) {
            throw new IllegalArgumentException("Available copies must be between 0 and " + totalCopies);
        }
        this.availableCopies = copies;
    }

    @Override
    public int compareTo(Item other) {
        return title.compareToIgnoreCase(other.title);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Item other)) {
            return false;
        }
        if (id != 0 && other.id != 0) {
            return id == other.id;
        }
        return getType() == other.getType()
                && title.equalsIgnoreCase(other.title)
                && author.equalsIgnoreCase(other.author);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), title.toLowerCase(), author.toLowerCase());
    }

    @Override
    public String toString() {
        return getType() + ": " + title + " by " + author;
    }
}
