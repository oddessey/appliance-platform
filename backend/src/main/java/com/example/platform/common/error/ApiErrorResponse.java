package com.example.platform.common.error;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        String requestId,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String reason) {
    }
}
