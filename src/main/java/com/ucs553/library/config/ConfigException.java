package com.ucs553.library.config;

import com.ucs553.library.exception.LibraryException;

public class ConfigException extends LibraryException {

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
