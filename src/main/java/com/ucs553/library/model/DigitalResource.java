package com.ucs553.library.model;

public class DigitalResource extends Item {

    private static final long serialVersionUID = 1L;

    private final String format;
    private final double sizeMb;

    public DigitalResource(String title, String author, int year, int licences, String format, double sizeMb) {
        super(title, author, year, licences);
        if (sizeMb < 0) {
            throw new IllegalArgumentException("File size cannot be negative");
        }
        this.format = format == null ? "" : format.trim().toUpperCase();
        this.sizeMb = sizeMb;
    }

    public String getFormat() {
        return format;
    }

    public double getSizeMb() {
        return sizeMb;
    }

    @Override
    public ItemType getType() {
        return ItemType.DIGITAL;
    }

    @Override
    public String describe() {
        return getTitle() + " by " + getAuthor() + " (" + getYear() + "), " + format + ", " + sizeMb + " MB";
    }
}
