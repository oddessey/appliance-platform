package com.example.platform.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordValidatorTest {

    private final PasswordValidator validator = new PasswordValidator();

    @Test
    void acceptsPasswordsWithinCharacterAndBcryptByteLimits() {
        assertTrue(validator.isValid("Strong-pass-123", null));
        assertTrue(validator.isValid("가나다라마바사아", null));
    }

    @Test
    void rejectsShortOrOverlongUtf8Passwords() {
        assertFalse(validator.isValid("short", null));
        assertFalse(validator.isValid("한".repeat(25), null));
    }
}
