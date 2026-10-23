package com.chiangraipass.auth.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Password rules for new email/password accounts: at least 8 characters, at least one digit,
 * at least one special character, and only English (printable ASCII) characters.
 * The returned message names every unmet rule so the visitor knows exactly what to fix.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72;

    private PasswordPolicy() {
    }

    public static Optional<String> validate(String password) {
        if (password == null || password.isEmpty()) {
            return Optional.of("Password is required.");
        }

        boolean hasDigit = false;
        boolean hasSpecial = false;
        boolean onlyEnglish = true;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (c < 0x20 || c > 0x7E) {
                onlyEnglish = false;
            } else if (c >= '0' && c <= '9') {
                hasDigit = true;
            } else if (c != ' ' && !isAsciiLetter(c)) {
                hasSpecial = true;
            }
        }

        List<String> unmet = new ArrayList<>();
        if (password.length() < MIN_LENGTH) {
            unmet.add("at least " + MIN_LENGTH + " characters");
        }
        if (password.length() > MAX_LENGTH) {
            unmet.add("at most " + MAX_LENGTH + " characters");
        }
        if (!hasDigit) {
            unmet.add("at least 1 number");
        }
        if (!hasSpecial) {
            unmet.add("at least 1 special character (for example ! @ # $ %)");
        }
        if (!onlyEnglish) {
            unmet.add("only English letters, numbers and symbols (no Thai or other languages)");
        }
        return unmet.isEmpty()
                ? Optional.empty()
                : Optional.of("Password must have: " + String.join("; ", unmet) + ".");
    }

    private static boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
}