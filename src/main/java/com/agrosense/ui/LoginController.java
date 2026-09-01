package com.agrosense.ui;

import com.agrosense.AgroSenseApp;
import com.agrosense.model.Customer;
import com.agrosense.session.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;

import java.util.Optional;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        themeToggleButton;

    @FXML
    public void initialize() {
        updateThemeButton();
    }

    @FXML
    private void onToggleTheme() {
        if (themeToggleButton.getScene() != null) {
            ThemeManager.toggleTheme(themeToggleButton.getScene().getRoot());
            updateThemeButton();
        }
    }

    private void updateThemeButton() {
        if (themeToggleButton != null) {
            themeToggleButton.setText(ThemeManager.isLightMode() ? "🌙" : "☀️");
        }
    }

    @FXML
    private void onLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isBlank() || password.isBlank()) {
            showError("Please enter your email and password.");
            return;
        }

        try {
            Optional<Customer> customerOpt = AgroSenseApp.authService.login(email, password);
            if (customerOpt.isEmpty()) {
                showError("Invalid email or password.");
                passwordField.clear();
                return;
            }
            SessionManager.getInstance().setCurrentCustomer(customerOpt.get());
            AgroSenseApp.navigateTo("main_layout");
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        }
    }

    @FXML
    private void goToRegister() {
        AgroSenseApp.navigateTo("register");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
