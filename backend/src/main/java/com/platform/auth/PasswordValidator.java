package com.platform.auth;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true;
        }
        return password.codePointCount(0, password.length()) >= 8
                && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
