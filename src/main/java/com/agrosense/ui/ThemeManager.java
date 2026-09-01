package com.agrosense.ui;

import javafx.scene.Parent;

public class ThemeManager {
    private static boolean isLightMode = false;

    public static boolean isLightMode() {
        return isLightMode;
    }

    public static void setLightMode(boolean lightMode) {
        isLightMode = lightMode;
    }

    public static void toggleTheme(Parent root) {
        isLightMode = !isLightMode;
        applyTheme(root);
    }

    public static void applyTheme(Parent root) {
        if (root == null) return;
        if (isLightMode) {
            if (!root.getStyleClass().contains("light-theme")) {
                root.getStyleClass().add("light-theme");
            }
        } else {
            root.getStyleClass().remove("light-theme");
        }
    }
}
