package com.ucs553.library.io;

import com.ucs553.library.model.Book;
import com.ucs553.library.model.Member;
import com.ucs553.library.service.LibraryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataExporterTest {

    @TempDir
    Path dir;

    private Fixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new Fixture(dir);
        LibraryService issuing = fixture.on(LocalDate.of(2026, 11, 1));
        Book book = new Book.Builder().title("Effective Java").author("Joshua Bloch").year(2018)
                .copies(2).isbn("X-1").pages(412).build();
        issuing.saveItem(book);
        Member member = new Member("Himmat", "himmat@example.com", LocalDate.of(2026, 1, 1), 5);
        issuing.saveMember(member);
        issuing.borrow(book.getId(), member.getId());
    }

    @Test
    void itemsFileHasHeaderAndOneRowPerItem() throws Exception {
        Path file = dir.resolve("items.csv");
        new DataExporter(fixture.on(LocalDate.of(2026, 11, 20))).exportItems(file);
        List<String> lines = Files.readAllLines(file);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).startsWith("type,title,author"));
        assertTrue(lines.get(1).startsWith("BOOK,Effective Java,Joshua Bloch,2018,2,1,X-1,412"));
    }

    @Test
    void membersFileContainsTheEmail() throws Exception {
        Path file = dir.resolve("members.csv");
        new DataExporter(fixture.on(LocalDate.of(2026, 11, 20))).exportMembers(file);
        assertTrue(Files.readString(file).contains("himmat@example.com"));
    }

    @Test
    void loansFileShowsOverdueStatusAndFine() throws Exception {
        Path file = dir.resolve("loans.csv");
        new DataExporter(fixture.on(LocalDate.of(2026, 11, 20))).exportLoans(file);
        String row = Files.readAllLines(file).get(1);
        assertTrue(row.contains("Overdue"));
        assertTrue(row.endsWith("25.00"));
    }

    @Test
    void overdueReportListsLateLoansWithTotal() throws Exception {
        String report = new DataExporter(fixture.on(LocalDate.of(2026, 11, 20))).overdueReport();
        assertTrue(report.contains("Effective Java"));
        assertTrue(report.contains("Himmat"));
        assertTrue(report.contains("total fines ₹25.00"));
    }

    @Test
    void overdueReportSaysWhenNothingIsLate() throws Exception {
        String report = new DataExporter(fixture.on(LocalDate.of(2026, 11, 2))).overdueReport();
        assertTrue(report.contains("No overdue loans"));
    }
}
