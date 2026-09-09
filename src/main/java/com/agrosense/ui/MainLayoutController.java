package com.agrosense.ui;

import com.agrosense.AgroSenseApp;
import com.agrosense.session.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * Controller for the main shell (sidebar + dynamic content pane).
 * Each nav button loads the appropriate sub-view into the center StackPane.
 */
public class MainLayoutController {

    private static MainLayoutController instance;

    public MainLayoutController() {
        instance = this;
    }

    public static MainLayoutController getInstance() {
        return instance;
    }

    @FXML private StackPane contentPane;
    @FXML private Button navDashboard;
    @FXML private Button navPairing;
    @FXML private Button navAlerts;
    @FXML private Button navHistory;
    @FXML private Button navAccount;
    @FXML private Button themeToggleButton;

    @FXML
    public void initialize() {
        showDashboard();
        updateThemeButtonText();
    }

    @FXML public void showDashboard()    { loadView("dashboard",    navDashboard); }
    @FXML public void showPairing()      { loadView("pairing",      navPairing); }
    @FXML public void showAlertRules()   { loadView("alert_rules",  navAlerts); }
    @FXML public void showAlertHistory() { loadView("alert_history",navHistory); }
    @FXML public void showAccount()      { loadView("account",      navAccount); }

    @FXML
    private void onSignOut() {
        SessionManager.getInstance().logout();
        AgroSenseApp.navigateTo("login");
    }

    @FXML
    private void onToggleTheme() {
        ThemeManager.toggleTheme(contentPane.getScene().getRoot());
        updateThemeButtonText();
    }

    private void updateThemeButtonText() {
        if (themeToggleButton != null) {
            themeToggleButton.setText(ThemeManager.isLightMode() ? "🌙 Dark Mode" : "☀️ Light Mode");
        }
    }

    private void loadView(String fxmlName, Button activeButton) {
        // Reset all nav buttons
        for (Button b : new Button[]{navDashboard, navPairing, navAlerts, navHistory, navAccount}) {
            b.getStyleClass().removeAll("nav-button-active");
            if (!b.getStyleClass().contains("nav-button")) b.getStyleClass().add("nav-button");
        }
        activeButton.getStyleClass().add("nav-button-active");

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/" + fxmlName + ".fxml"));
            Node view = loader.load();
            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Called by child controllers (e.g., Dashboard → SiteDetail) */
    public void loadViewNode(Node node) {
        contentPane.getChildren().setAll(node);
    }
}
