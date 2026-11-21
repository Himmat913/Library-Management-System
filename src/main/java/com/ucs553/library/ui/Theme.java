package com.ucs553.library.ui;

import com.ucs553.library.config.UserSettings;
import javafx.scene.Parent;
import javafx.scene.control.Button;

// light or dark look: the stylesheet changes its colours when the scene root has the "dark" class
public final class Theme {

    private static final String DARK = "dark";

    private Theme() {
    }

    public static void apply(Parent root, boolean dark) {
        root.getStyleClass().remove(DARK);
        if (dark) {
            root.getStyleClass().add(DARK);
        }
    }

    static Button toggleButton(UserSettings settings) {
        Button button = new Button();
        button.setMaxWidth(Double.MAX_VALUE);
        button.setText(settings.isDarkMode() ? "Light mode" : "Dark mode");
        button.setOnAction(e -> {
            settings.setDarkMode(!settings.isDarkMode());
            button.setText(settings.isDarkMode() ? "Light mode" : "Dark mode");
            apply(button.getScene().getRoot(), settings.isDarkMode());
        });
        return button;
    }
}
