package com.platform.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 30) @Pattern(regexp = "(?=.*\\d)\\+?[0-9() -]{7,29}") String phone
) {
}
