package com.ucs553.library.io;

import java.util.List;

public record ImportResult(int imported, List<String> errors) {

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
