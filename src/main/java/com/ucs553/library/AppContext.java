package com.ucs553.library;

import com.ucs553.library.config.AppConfig;
import com.ucs553.library.service.LibraryService;

public final class AppContext {

    private static AppContext instance;

    private final AppConfig config;
    private final LibraryService service;

    private AppContext(AppConfig config, LibraryService service) {
        this.config = config;
        this.service = service;
    }

    public static synchronized void init(AppConfig config, LibraryService service) {
        instance = new AppContext(config, service);
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
}
