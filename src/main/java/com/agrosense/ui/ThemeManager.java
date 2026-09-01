package com.agrosense.ui;

import javafx.scene.Parent;
import java.util.prefs.Preferences;

public class ThemeManager {
    private static final String PREF_KEY = "isLightMode";
    private static Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
    private static boolean isLightMode = prefs.getBoolean(PREF_KEY, false);

    public static boolean isLightMode() {
        return isLightMode;
    }

    public static void setLightMode(boolean lightMode) {
        isLightMode = lightMode;
        prefs.putBoolean(PREF_KEY, lightMode);
    }

    public static void toggleTheme(Parent root) {
        setLightMode(!isLightMode);
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
