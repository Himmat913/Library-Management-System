package com.ucs553.library.model;

public enum ItemType {
    BOOK("Book", 14),
    MAGAZINE("Magazine", 7),
    DIGITAL("Digital resource", 21);

    private final String label;
    private final int loanDays;

    ItemType(String label, int loanDays) {
        this.label = label;
        this.loanDays = loanDays;
    }

    public int getLoanDays() {
        return loanDays;
    }

    @Override
    public String toString() {
        return label;
    }
}
