package com.ucs553.library.dao;

import java.nio.file.Path;

final class TestDb {

    private TestDb() {
    }

    static Database create(Path dir) throws DataAccessException {
        Database db = new Database("jdbc:sqlite:" + dir.resolve("test.db"));
        db.initSchema();
        return db;
    }
}
