package com.ucs553.library.dao;

import com.ucs553.library.model.Book;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseBackupTest {

    @TempDir
    Path dir;

    @Test
    void backupHoldsTheSameData() throws Exception {
        Database db = TestDb.create(dir);
        new JdbcItemDao(db).insert(new Book.Builder().title("A").author("B").year(2020).build());

        Path backup = dir.resolve("backups").resolve("copy.db");
        db.backupTo(backup);

        assertTrue(Files.exists(backup));
        Database copy = new Database("jdbc:sqlite:" + backup);
        assertEquals(1, new JdbcItemDao(copy).findAll().size());
    }
}
