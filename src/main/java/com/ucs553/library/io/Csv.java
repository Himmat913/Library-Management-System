package com.ucs553.library.io;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class Csv {

    private Csv() {
    }

    public static String join(List<String> fields) {
        return fields.stream().map(Csv::quote).collect(Collectors.joining(","));
    }

    private static String quote(String field) {
        if (field == null) {
            return "";
        }
        String text = field.replace('\r', ' ').replace('\n', ' ');
        if (text.contains(",") || text.contains("\"")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    public static List<String> parse(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
