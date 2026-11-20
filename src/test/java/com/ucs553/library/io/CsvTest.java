package com.ucs553.library.io;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvTest {

    @Test
    void fieldsWithCommasAndQuotesAreQuoted() {
        assertEquals("a,\"b,c\",\"say \"\"hi\"\"\"", Csv.join(List.of("a", "b,c", "say \"hi\"")));
    }

    @Test
    void parseUndoesQuoting() {
        assertEquals(List.of("a", "b,c", "say \"hi\""), Csv.parse("a,\"b,c\",\"say \"\"hi\"\"\""));
    }

    @Test
    void emptyFieldsAreKept() {
        assertEquals(List.of("a", "", "c", ""), Csv.parse("a,,c,"));
    }
}
