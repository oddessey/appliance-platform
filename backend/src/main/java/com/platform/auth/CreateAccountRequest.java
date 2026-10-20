package com.platform.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @ValidPassword String password,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 30) @Pattern(regexp = "(?=.*\\d)\\+?[0-9() -]{7,29}") String phone,
        SignupAccountType accountType
) {
    public CreateAccountRequest {
        if (accountType == null) {
            accountType = SignupAccountType.CUSTOMER;
        }
    }

    public CreateAccountRequest(String email, String password, String name, String phone) {
        this(email, password, name, phone, SignupAccountType.CUSTOMER);
    }
}
