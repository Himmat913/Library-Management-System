package com.ucs553.library;

import com.ucs553.library.config.AppConfig;
import com.ucs553.library.dao.Database;
import com.ucs553.library.service.LibraryService;

public final class AppContext {

    private static AppContext instance;

    private final AppConfig config;
    private final LibraryService service;
    private final Database database;

    private AppContext(AppConfig config, LibraryService service, Database database) {
        this.config = config;
        this.service = service;
        this.database = database;
    }

    public static synchronized void init(AppConfig config, LibraryService service, Database database) {
        instance = new AppContext(config, service, database);
    }

    public static synchronized AppContext get() {
        if (instance == null) {
            throw new IllegalStateException("AppContext has not been initialised");
        }
        return instance;
    }

    public AppConfig config() {
        return config;
    }

    public LibraryService service() {
        return service;
    }

    public Database database() {
        return database;
    }
}
