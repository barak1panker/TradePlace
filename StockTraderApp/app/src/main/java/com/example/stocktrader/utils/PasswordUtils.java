package com.example.stocktrader.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for hashing passwords with SHA-256 + salt.
 * We don't store plain-text passwords. The salt is the username, lowercased.
 */
public class PasswordUtils {

    private PasswordUtils() {}

    public static String hash(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = (salt == null ? "" : salt.toLowerCase()) + ":" + password;
            byte[] bytes = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    /**
     * Minimal password rule: at least 6 characters.
     */
    public static boolean isValidPassword(String pwd) {
        return pwd != null && pwd.length() >= 6;
    }

    /**
     * Username rule: 3-20 chars, alphanumeric + underscore.
     */
    public static boolean isValidUsername(String username) {
        return username != null && username.matches("^[a-zA-Z0-9_]{3,20}$");
    }
}
