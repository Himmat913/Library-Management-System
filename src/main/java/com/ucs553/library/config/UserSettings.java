package com.ucs553.library.config;

import java.io.Serializable;
import java.time.LocalDateTime;

public class UserSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final double DEFAULT_WIDTH = 1100;
    public static final double DEFAULT_HEIGHT = 700;

    private double windowWidth = DEFAULT_WIDTH;
    private double windowHeight = DEFAULT_HEIGHT;
    private String lastPage = "Dashboard";
    private String lastUsername = "";
    private boolean darkMode;
    private LocalDateTime savedAt;

    // only says whether this copy was edited since it was loaded, so it is not worth saving
    private transient boolean changed;

    public double getWindowWidth() {
        return windowWidth;
    }

    public double getWindowHeight() {
        return windowHeight;
    }

    public void setWindowSize(double width, double height) {
        this.windowWidth = width;
        this.windowHeight = height;
        changed = true;
    }

    public String getLastPage() {
        return lastPage;
    }

    public void setLastPage(String lastPage) {
        this.lastPage = lastPage;
        changed = true;
    }

    public String getLastUsername() {
        return lastUsername;
    }

    public void setLastUsername(String lastUsername) {
        this.lastUsername = lastUsername;
        changed = true;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        changed = true;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    void stamp() {
        this.savedAt = LocalDateTime.now();
    }

    public boolean isChanged() {
        return changed;
    }
}
