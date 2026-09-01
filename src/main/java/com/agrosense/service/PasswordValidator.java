package com.agrosense.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates password strength against mandatory criteria.
 *
 * Rules:
 *   ✓ At least 8 characters
 *   ✓ At least 1 uppercase letter (A–Z)
 *   ✓ At least 1 lowercase letter (a–z)
 *   ✓ At least 1 digit (0–9)
 *   ✓ At least 1 special character (!@#$%^&*()-_=+[]{}|;:,.<>?/~`)
 *   ✗ No spaces allowed
 */
public final class PasswordValidator {

    private PasswordValidator() {}

    public static final String SPECIAL_CHARS = "!@#$%^&*()-_=+[]{}|;:,.<>?/~`";

    public record ValidationResult(boolean valid, List<String> failedCriteria) {
        public String firstError() {
            return failedCriteria.isEmpty() ? null : failedCriteria.get(0);
        }
    }

    public static ValidationResult validate(String password) {
        List<String> failed = new ArrayList<>();

        if (password == null || password.length() < 8)
            failed.add("At least 8 characters");

        if (password != null && password.contains(" "))
            failed.add("No spaces allowed");

        if (password == null || !password.chars().anyMatch(Character::isUpperCase))
            failed.add("At least 1 uppercase letter (A–Z)");

        if (password == null || !password.chars().anyMatch(Character::isLowerCase))
            failed.add("At least 1 lowercase letter (a–z)");

        if (password == null || !password.chars().anyMatch(Character::isDigit))
            failed.add("At least 1 digit (0–9)");

        if (password == null || password.chars().noneMatch(c -> SPECIAL_CHARS.indexOf(c) >= 0))
            failed.add("At least 1 special character (" + SPECIAL_CHARS + ")");

        return new ValidationResult(failed.isEmpty(), failed);
    }

    /** Returns a user-friendly strength label for UI display. */
    public static String strengthLabel(String password) {
        if (password == null || password.isBlank()) return "";
        int score = 0;
        if (password.length() >= 8)  score++;
        if (password.length() >= 12) score++;
        if (password.chars().anyMatch(Character::isUpperCase)) score++;
        if (password.chars().anyMatch(Character::isLowerCase)) score++;
        if (password.chars().anyMatch(Character::isDigit))     score++;
        if (password.chars().anyMatch(c -> SPECIAL_CHARS.indexOf(c) >= 0)) score++;
        return switch (score) {
            case 0, 1 -> "Very Weak";
            case 2    -> "Weak";
            case 3, 4 -> "Fair";
            case 5    -> "Strong";
            default   -> "Very Strong";
        };
    }

    public static String strengthStyle(String password) {
        return switch (strengthLabel(password)) {
            case "Very Weak", "Weak" -> "-fx-text-fill: -color-danger;";
            case "Fair"              -> "-fx-text-fill: -color-warn;";
            default                  -> "-fx-text-fill: -color-accent;";
        };
    }
}
