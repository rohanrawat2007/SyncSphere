package com.syncsphere.util;

import java.util.regex.Pattern;

public final class ValidationUtil {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    private ValidationUtil() {
    }

    public static void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("Username must be 3-20 chars, letters/numbers/underscore only.");
        }
    }

    public static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException("Password must be at least 8 chars and include uppercase, lowercase, number, and symbol.");
        }
    }

    public static void validateMessage(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty.");
        }
        if (content.trim().length() > 500) {
            throw new IllegalArgumentException("Message length cannot exceed 500 characters.");
        }
    }
}
