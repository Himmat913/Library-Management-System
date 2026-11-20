package com.ucs553.library.io;

import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.service.LibraryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DataImporterTest {

    @TempDir
    Path dir;

    private LibraryService service;
    private DataImporter importer;

    @BeforeEach
    void setUp() throws Exception {
        service = new Fixture(dir).on(LocalDate.of(2026, 11, 1));
        importer = new DataImporter(service);
    }

    private Path write(String name, String text) throws Exception {
        Path file = dir.resolve(name);
        Files.writeString(file, text);
        return file;
    }

    @Test
    void goodRowsAreImportedAndBadRowsReported() throws Exception {
        Path file = write("items.csv", """
                type,title,author,year,copies,isbn,pages
                BOOK,"Clean Code, 2nd",Robert Martin,2008,2,DEMO,464
                MAGAZINE,Wired,Various,2026,1,,
                SOMETHING,X,Y,2020,1,,
                BOOK,,Nobody,2020,1,,
                BOOK,Bad Year,Z,abc,1,,
                """);
        ImportResult result = importer.importItems(file);
        assertEquals(2, result.imported());
        assertEquals(3, result.errors().size());
        assertTrue(result.errors().get(0).startsWith("Line 4"));
        assertEquals(2, service.allItems().size());
    }

    @Test
    void importingTheSameFileTwiceAddsNothing() throws Exception {
        Path file = write("items.csv", "type,title,author,year,copies\nBOOK,Clean Code,Robert Martin,2008,2\n");
        assertEquals(1, importer.importItems(file).imported());
        ImportResult again = importer.importItems(file);
        assertEquals(0, again.imported());
        assertEquals(1, again.errors().size());
        assertEquals(1, service.allItems().size());
    }

    @Test
    void missingColumnStopsTheImport() throws Exception {
        Path file = write("items.csv", "type,title,year,copies\nBOOK,Clean Code,2008,2\n");
        assertThrows(LibraryException.class, () -> importer.importItems(file));
    }

    @Test
    void membersAreImportedWithDefaults() throws Exception {
        Path file = write("members.csv", """
                name,email
                A,a@example.com
                B,not-an-email
                C,a@example.com
                """);
        ImportResult result = importer.importMembers(file);
        assertEquals(1, result.imported());
        assertEquals(2, result.errors().size());
        assertEquals(5, service.allMembers().get(0).getBorrowLimit());
    }
}
