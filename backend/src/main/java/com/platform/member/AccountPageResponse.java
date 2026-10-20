package com.platform.member;

import java.util.List;

public record AccountPageResponse(
        int page,
        int size,
        long totalElements,
        List<AccountResponse> content
) {
}
