package com.ucs553.library;

import javafx.application.Application;

public class Launcher {

    // separate from LibraryApp so the app also starts from a plain jar
    public static void main(String[] args) {
        try {
            LaunchOptions.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("usage: [--config <file>] [--reset-db] [--seed-demo]");
            System.exit(2);
        }
        Application.launch(LibraryApp.class, args);
    }
}
