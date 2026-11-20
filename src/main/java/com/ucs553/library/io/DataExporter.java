package com.ucs553.library.io;

import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.Book;
import com.ucs553.library.model.DigitalResource;
import com.ucs553.library.model.Item;
import com.ucs553.library.model.LoanStatus;
import com.ucs553.library.model.Magazine;
import com.ucs553.library.model.Member;
import com.ucs553.library.service.LibraryService;
import com.ucs553.library.service.LoanDetail;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class DataExporter {

    private final LibraryService service;

    public DataExporter(LibraryService service) {
        this.service = service;
    }

    public void exportItems(Path file) throws IOException, LibraryException {
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            line(out, "type", "title", "author", "year", "copies", "available",
                    "isbn", "pages", "issue_number", "publisher", "format", "size_mb");
            for (Item item : service.allItems()) {
                String isbn = "";
                String pages = "";
                String issue = "";
                String publisher = "";
                String format = "";
                String size = "";
                if (item instanceof Book b) {
                    isbn = b.getIsbn();
                    pages = String.valueOf(b.getPages());
                } else if (item instanceof Magazine m) {
                    issue = String.valueOf(m.getIssueNumber());
                    publisher = m.getPublisher();
                } else if (item instanceof DigitalResource d) {
                    format = d.getFormat();
                    size = String.valueOf(d.getSizeMb());
                }
                line(out, item.getType().name(), item.getTitle(), item.getAuthor(),
                        String.valueOf(item.getYear()), String.valueOf(item.getTotalCopies()),
                        String.valueOf(item.getAvailableCopies()), isbn, pages, issue, publisher, format, size);
            }
        }
    }

    public void exportMembers(Path file) throws IOException, LibraryException {
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            line(out, "name", "email", "joined_on", "borrow_limit");
            for (Member m : service.allMembers()) {
                line(out, m.getName(), m.getEmail(), m.getJoinedOn().toString(), String.valueOf(m.getBorrowLimit()));
            }
        }
    }

    public void exportLoans(Path file) throws IOException, LibraryException {
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            line(out, "loan_id", "item", "member", "issued_on", "due_on", "returned_on", "status", "fine");
            for (LoanDetail d : service.loanDetails()) {
                line(out, String.valueOf(d.loan().getId()), d.itemTitle(), d.memberName(),
                        d.loan().getIssuedOn().toString(), d.loan().getDueOn().toString(),
                        d.loan().getReturnedOn() == null ? "" : d.loan().getReturnedOn().toString(),
                        statusText(d.status()), d.fine().toPlainString());
            }
        }
    }

    public void writeOverdueReport(Path file) throws IOException, LibraryException {
        Files.writeString(file, overdueReport(), StandardCharsets.UTF_8);
    }

    public String overdueReport() throws LibraryException {
        List<LoanDetail> overdue = service.loanDetails().stream()
                .filter(d -> d.status() == LoanStatus.OVERDUE)
                .sorted(Comparator.comparing(d -> d.loan().getDueOn()))
                .toList();

        StringBuilder text = new StringBuilder();
        text.append("OVERDUE LOANS REPORT\n");
        text.append("As of ").append(service.today()).append("\n\n");
        if (overdue.isEmpty()) {
            text.append("No overdue loans.\n");
            return text.toString();
        }

        text.append(String.format("%-30s %-20s %-12s %9s %10s%n", "Item", "Member", "Due", "Days late", "Fine"));
        text.append("-".repeat(85)).append('\n');
        BigDecimal total = BigDecimal.ZERO;
        for (LoanDetail d : overdue) {
            text.append(String.format("%-30s %-20s %-12s %9d %10s%n",
                    fit(d.itemTitle(), 30), fit(d.memberName(), 20), d.loan().getDueOn(),
                    d.loan().overdueDays(service.today()), "₹" + d.fine()));
            total = total.add(d.fine());
        }
        text.append("-".repeat(85)).append('\n');
        text.append(overdue.size()).append(" overdue loans, total fines ₹").append(total.setScale(2)).append('\n');
        return text.toString();
    }

    private static String statusText(LoanStatus status) {
        return switch (status) {
            case ACTIVE -> "On loan";
            case OVERDUE -> "Overdue";
            case RETURNED -> "Returned";
        };
    }

    private static String fit(String text, int width) {
        return text.length() <= width ? text : text.substring(0, width - 3) + "...";
    }

    private static void line(BufferedWriter out, String... fields) throws IOException {
        out.write(Csv.join(List.of(fields)));
        out.newLine();
    }
}
