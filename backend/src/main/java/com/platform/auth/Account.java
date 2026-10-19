package com.platform.auth;

import java.util.List;
import java.util.UUID;

public record Account(
        UUID id,
        String email,
        String passwordHash,
        String name,
        String phone,
        String status,
        List<String> roles
) {
}
