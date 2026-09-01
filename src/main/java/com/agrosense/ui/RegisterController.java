package com.agrosense.ui;

import com.agrosense.AgroSenseApp;
import com.agrosense.model.Customer;
import com.agrosense.service.PasswordValidator;
import com.agrosense.session.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;

public class RegisterController {

    @FXML private TextField     nameField;
    @FXML private TextField     phoneField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         strengthLabel;
    @FXML private Label         matchLabel;
    @FXML private Label         errorLabel;

    // Criteria indicator labels
    @FXML private Label crit8chars;
    @FXML private Label critUpper;
    @FXML private Label critLower;
    @FXML private Label critDigit;
    @FXML private Label critSpecial;
    @FXML private Label critNoSpaces;
    @FXML private Button themeToggleButton;

    @FXML
    public void initialize() {
        // Live password validation feedback as user types
        passwordField.textProperty().addListener((obs, old, newVal) -> {
            updateCriteriaDisplay(newVal);
            updateStrength(newVal);
            checkMatch();
        });
        confirmPasswordField.textProperty().addListener((obs, old, newVal) -> checkMatch());
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
    private void onRegister() {
        String name     = nameField.getText().trim();
        String phone    = phoneField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();

        hideError();

        try {
            Customer customer = AgroSenseApp.authService.register(name, phone, email, password, confirm);
            SessionManager.getInstance().setCurrentCustomer(customer);
            AgroSenseApp.navigateTo("main_layout");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Registration failed: " + e.getMessage());
        }
    }

    @FXML
    private void goToLogin() {
        AgroSenseApp.navigateTo("login");
    }

    // ── private helpers ───────────────────────────────────────────────

    private void updateCriteriaDisplay(String password) {
        setCriterion(crit8chars,   password.length() >= 8,          "At least 8 characters");
        setCriterion(critUpper,    password.chars().anyMatch(Character::isUpperCase), "At least 1 uppercase (A–Z)");
        setCriterion(critLower,    password.chars().anyMatch(Character::isLowerCase), "At least 1 lowercase (a–z)");
        setCriterion(critDigit,    password.chars().anyMatch(Character::isDigit),     "At least 1 digit (0–9)");
        setCriterion(critSpecial,  password.chars().anyMatch(c -> PasswordValidator.SPECIAL_CHARS.indexOf(c) >= 0),
                                   "At least 1 special character");
        setCriterion(critNoSpaces, !password.contains(" "),          "No spaces");
    }

    private void setCriterion(Label label, boolean met, String text) {
        if (met) {
            label.setText("✓  " + text);
            label.setStyle("-fx-text-fill: -color-accent;");
        } else {
            label.setText("○  " + text);
            label.setStyle("-fx-text-fill: -color-text-secondary;");
        }
    }

    private void updateStrength(String password) {
        if (password.isEmpty()) {
            strengthLabel.setText("");
            return;
        }
        String label = PasswordValidator.strengthLabel(password);
        strengthLabel.setText(label);
        strengthLabel.setStyle(PasswordValidator.strengthStyle(password));
    }

    private void checkMatch() {
        String p = passwordField.getText();
        String c = confirmPasswordField.getText();
        if (c.isEmpty()) {
            matchLabel.setVisible(false); matchLabel.setManaged(false);
            return;
        }
        if (p.equals(c)) {
            matchLabel.setText("✓ Passwords match");
            matchLabel.setStyle("-fx-text-fill: -color-accent;");
        } else {
            matchLabel.setText("✗ Passwords do not match");
            matchLabel.setStyle("");
        }
        matchLabel.setVisible(true); matchLabel.setManaged(true);
    }

    private void showError(String msg) {
        errorLabel.setText("✗ " + msg);
        errorLabel.setVisible(true); errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false); errorLabel.setManaged(false);
    }
}
