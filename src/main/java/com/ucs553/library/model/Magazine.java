package com.ucs553.library.model;

public class Magazine extends Item {

    private static final long serialVersionUID = 1L;

    private final int issueNumber;
    private final String publisher;

    public Magazine(String title, String author, int year, int copies, int issueNumber, String publisher) {
        super(title, author, year, copies);
        this.issueNumber = issueNumber;
        this.publisher = publisher == null ? "" : publisher.trim();
    }

    public int getIssueNumber() {
        return issueNumber;
    }

    public String getPublisher() {
        return publisher;
    }

    @Override
    public ItemType getType() {
        return ItemType.MAGAZINE;
    }

    @Override
    public String describe() {
        return getTitle() + ", issue " + issueNumber + " (" + getYear() + "), published by " + publisher;
    }
}
