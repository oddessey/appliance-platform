package com.platform.common.error;

import com.platform.auth.AuthenticationFailedException;
import com.platform.auth.EmailAlreadyRegisteredException;
import com.platform.installation.UnknownApplianceTypeException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldError> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();
        return response(request, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "요청 값이 올바르지 않습니다.", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpServletRequest request) {
        return response(request, HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "요청 본문 형식이 올바르지 않습니다.", List.of());
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(HttpServletRequest request) {
        return response(request, HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED",
                "이미 사용 중인 이메일입니다.", List.of());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationFailure(HttpServletRequest request) {
        return response(request, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED",
                "이메일 또는 비밀번호가 올바르지 않습니다.", List.of());
    }

    @ExceptionHandler(UnknownApplianceTypeException.class)
    public ResponseEntity<ApiErrorResponse> handleUnknownApplianceType(HttpServletRequest request) {
        return response(request, HttpStatus.UNPROCESSABLE_ENTITY, "UNKNOWN_APPLIANCE_TYPE",
                "선택한 가전제품 종류를 사용할 수 없습니다.", List.of());
    }

    private ApiErrorResponse.FieldError toFieldError(FieldError error) {
        return new ApiErrorResponse.FieldError(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String message,
            List<ApiErrorResponse.FieldError> fieldErrors
    ) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        return ResponseEntity.status(status).body(new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                requestId,
                fieldErrors
        ));
    }
}
