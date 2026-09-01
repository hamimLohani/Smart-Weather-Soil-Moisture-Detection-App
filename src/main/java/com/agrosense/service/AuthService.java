package com.agrosense.service;

import com.agrosense.dao.CustomerDAO;
import com.agrosense.model.Customer;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Handles customer authentication and registration with strong password enforcement.
 */
public class AuthService {

    private final CustomerDAO customerDAO;

    public AuthService(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
    }

    /**
     * Authenticates a customer by email + password.
     * @return Customer if credentials are valid, empty otherwise
     */
    public Optional<Customer> login(String email, String password) throws SQLException {
        if (email == null || email.isBlank() || password == null || password.isBlank())
            return Optional.empty();

        Optional<Customer> customerOpt = customerDAO.findByEmail(email.trim().toLowerCase());
        if (customerOpt.isEmpty()) return Optional.empty();

        Customer customer = customerOpt.get();

        // Handle legacy seed accounts that may have no password yet (salt == null)
        if (customer.getSalt() == null || customer.getPasswordHash() == null) {
            return Optional.empty();
        }

        if (!PasswordUtils.verifyPassword(password, customer.getPasswordHash(), customer.getSalt())) {
            return Optional.empty();
        }

        return Optional.of(customer);
    }

    /**
     * Registers a new customer with strong password validation.
     * @throws IllegalArgumentException for validation failures (displayed inline in the UI)
     */
    public Customer register(String name, String phone, String email,
                             String password, String confirmPassword) throws SQLException {
        // Input presence
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Full name is required.");
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email is required.");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Password is required.");
        if (confirmPassword == null || confirmPassword.isBlank())
            throw new IllegalArgumentException("Please confirm your password.");

        // Password match
        if (!password.equals(confirmPassword))
            throw new IllegalArgumentException("Passwords do not match.");

        // Password strength
        PasswordValidator.ValidationResult vr = PasswordValidator.validate(password);
        if (!vr.valid()) {
            throw new IllegalArgumentException("Password does not meet requirements: "
                + String.join(", ", vr.failedCriteria()));
        }

        // Email uniqueness
        String normalizedEmail = email.trim().toLowerCase();
        if (customerDAO.findByEmail(normalizedEmail).isPresent())
            throw new IllegalArgumentException("An account with this email already exists.");

        // Hash password and persist
        String salt         = PasswordUtils.generateSalt();
        String passwordHash = PasswordUtils.hashPassword(password, salt);

        return customerDAO.insert(name.trim(), phone == null ? "" : phone.trim(),
                                  normalizedEmail, passwordHash, salt);
    }

    /**
     * Updates a customer's password after verifying the current one.
     */
    public void changePassword(Customer customer, String currentPassword,
                               String newPassword, String confirmNew) throws SQLException {
        if (!PasswordUtils.verifyPassword(currentPassword, customer.getPasswordHash(), customer.getSalt()))
            throw new IllegalArgumentException("Current password is incorrect.");

        if (!newPassword.equals(confirmNew))
            throw new IllegalArgumentException("New passwords do not match.");

        PasswordValidator.ValidationResult vr = PasswordValidator.validate(newPassword);
        if (!vr.valid())
            throw new IllegalArgumentException("New password does not meet requirements: "
                + String.join(", ", vr.failedCriteria()));

        String newSalt = PasswordUtils.generateSalt();
        String newHash = PasswordUtils.hashPassword(newPassword, newSalt);
        customer.setSalt(newSalt);
        customer.setPasswordHash(newHash);
        customerDAO.update(customer);
    }
}
