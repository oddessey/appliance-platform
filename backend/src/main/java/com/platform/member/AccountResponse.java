package com.platform.member;

import java.util.List;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String email,
        String name,
        String phone,
        String status,
        List<String> roles
) {
}
