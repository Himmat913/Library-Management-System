package com.ucs553.library.model;

public class Book extends Item {

    private static final long serialVersionUID = 1L;

    private final String isbn;
    private final int pages;

    private Book(Builder b) {
        super(b.title, b.author, b.year, b.copies);
        this.isbn = b.isbn;
        this.pages = b.pages;
    }

    public String getIsbn() {
        return isbn;
    }

    public int getPages() {
        return pages;
    }

    @Override
    public ItemType getType() {
        return ItemType.BOOK;
    }

    @Override
    public String describe() {
        return getTitle() + " by " + getAuthor() + " (" + getYear() + "), ISBN " + isbn + ", " + pages + " pages";
    }

    public static class Builder {
        private String title;
        private String author;
        private int year;
        private int copies = 1;
        private String isbn = "";
        private int pages;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder year(int year) {
            this.year = year;
            return this;
        }

        public Builder copies(int copies) {
            this.copies = copies;
            return this;
        }

        public Builder isbn(String isbn) {
            this.isbn = isbn == null ? "" : isbn.trim();
            return this;
        }

        public Builder pages(int pages) {
            this.pages = pages;
            return this;
        }

        public Book build() {
            return new Book(this);
        }
    }
}
