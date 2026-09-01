package com.agrosense.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for password hashing (SHA-256 + per-user random salt).
 * SHA-256 with a 16-byte random salt is used to avoid adding a BCrypt dependency.
 */
public final class PasswordUtils {

    private PasswordUtils() {}

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Generates a cryptographically secure random 16-byte salt, Base64-encoded. */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes a plaintext password with the provided salt using SHA-256.
     * @return Base64-encoded hash string
     */
    public static String hashPassword(String plaintext, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // Include salt in the digest
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hash = md.digest(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Verifies a plaintext password against a stored hash and salt.
     * Uses constant-time comparison to prevent timing attacks.
     */
    public static boolean verifyPassword(String plaintext, String storedHash, String salt) {
        String candidateHash = hashPassword(plaintext, salt);
        // Constant-time comparison
        if (candidateHash.length() != storedHash.length()) return false;
        int diff = 0;
        for (int i = 0; i < candidateHash.length(); i++) {
            diff |= candidateHash.charAt(i) ^ storedHash.charAt(i);
        }
        return diff == 0;
    }
}
