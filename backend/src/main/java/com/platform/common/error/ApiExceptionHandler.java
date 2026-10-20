package com.platform.common.error;

import com.platform.auth.AuthenticationFailedException;
import com.platform.auth.AdminBootstrapDisabledException;
import com.platform.auth.EmailAlreadyRegisteredException;
import com.platform.auth.InvalidAdminBootstrapKeyException;
import com.platform.installation.UnknownApplianceTypeException;
import com.platform.installation.InvalidInstallationPageException;
import com.platform.member.InvalidMemberRequestException;
import com.platform.member.AccountManagementConflictException;
import com.platform.member.MemberNotFoundException;
import com.platform.pricing.InvalidPriceRuleException;
import com.platform.pricing.InvalidPricingPageException;
import com.platform.pricing.PriceRuleConflictException;
import com.platform.pricing.PriceRuleNotConfiguredException;
import com.platform.pricing.PriceRuleNotFoundException;
import com.platform.pricing.PricingRequestNotFoundException;
import com.platform.pricing.QuoteNotAcceptableException;
import com.platform.pricing.QuoteNotFoundException;
import com.platform.reservation.ActiveReservationExistsException;
import com.platform.reservation.IdempotencyKeyConflictException;
import com.platform.reservation.InvalidReservationException;
import com.platform.reservation.InvalidReservationPageException;
import com.platform.reservation.ReservationConflictException;
import com.platform.reservation.ReservationNotAcceptableException;
import com.platform.reservation.ReservationNotFoundException;
import com.platform.technician.AssignmentNotFoundException;
import com.platform.technician.InvalidAssignmentPageException;
import com.platform.technician.InvalidAssignmentTransitionException;
import com.platform.technician.TechnicianProfileUnavailableException;
import com.platform.technician.TechnicianProfileNotFoundException;
import com.platform.technician.InvalidTechnicianProfileException;
import com.platform.technician.InvalidTechnicianAssignmentException;
import com.platform.technician.InvalidTechnicianEvidenceException;
import com.platform.technician.TechnicianEvidenceStorageException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

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

    @ExceptionHandler(InvalidAdminBootstrapKeyException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidAdminBootstrapKey(HttpServletRequest request) {
        return response(request, HttpStatus.FORBIDDEN, "BOOTSTRAP_KEY_INVALID",
                "관리자 등록 키가 올바르지 않습니다.", List.of());
    }

    @ExceptionHandler(AdminBootstrapDisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleAdminBootstrapDisabled(HttpServletRequest request) {
        return response(request, HttpStatus.CONFLICT, "ADMIN_BOOTSTRAP_DISABLED",
                "초기 관리자 등록은 비활성화되어 있습니다.", List.of());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationFailure(HttpServletRequest request) {
        return response(request, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED",
                "이메일 또는 비밀번호가 올바르지 않습니다.", List.of());
    }

    @ExceptionHandler(InvalidPriceRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPriceRule(HttpServletRequest request) {
        return response(request, HttpStatus.BAD_REQUEST, "INVALID_PRICE_RULE",
                "가격 규칙의 조건, 계산 방식 또는 유효 기간이 올바르지 않습니다.", List.of());
    }

    @ExceptionHandler(PriceRuleConflictException.class)
    public ResponseEntity<ApiErrorResponse> handlePriceRuleConflict(HttpServletRequest request) {
        return response(request, HttpStatus.CONFLICT, "PRICE_RULE_CONFLICT",
                "이미 사용 중인 가격 규칙 코드입니다.", List.of());
    }

    @ExceptionHandler(PriceRuleNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePriceRuleNotFound(HttpServletRequest request) {
        return response(request, HttpStatus.NOT_FOUND, "PRICE_RULE_NOT_FOUND",
                "가격 규칙을 찾을 수 없습니다.", List.of());
    }

    @ExceptionHandler(PriceRuleNotConfiguredException.class)
    public ResponseEntity<ApiErrorResponse> handlePriceRuleNotConfigured(HttpServletRequest request) {
        return response(request, HttpStatus.UNPROCESSABLE_ENTITY, "PRICE_RULE_NOT_CONFIGURED",
                "요청 가전의 기본 가격 규칙이 설정되지 않았습니다.", List.of());
    }

    @ExceptionHandler(PricingRequestNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePricingRequestNotFound(HttpServletRequest request) {
        return response(request, HttpStatus.NOT_FOUND, "PRICING_REQUEST_NOT_FOUND",
                "견적 대상 요청을 찾을 수 없습니다.", List.of());
    }

    @ExceptionHandler(QuoteNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleQuoteNotFound(HttpServletRequest request) {
        return response(request, HttpStatus.NOT_FOUND, "QUOTE_NOT_FOUND",
                "견적을 찾을 수 없습니다.", List.of());
    }

    @ExceptionHandler(QuoteNotAcceptableException.class)
    public ResponseEntity<ApiErrorResponse> handleQuoteNotAcceptable(HttpServletRequest request) {
        return response(request, HttpStatus.CONFLICT, "QUOTE_NOT_ACCEPTABLE",
                "현재 견적은 수락할 수 없거나 다시 발행해야 합니다.", List.of());
    }

    @ExceptionHandler(InvalidPricingPageException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPricingPage(HttpServletRequest request) {
        return response(request, HttpStatus.BAD_REQUEST, "INVALID_PAGE",
                "페이지 번호 또는 페이지 크기가 올바르지 않습니다.", List.of());
    }

    @ExceptionHandler(UnknownApplianceTypeException.class)
    public ResponseEntity<ApiErrorResponse> handleUnknownApplianceType(HttpServletRequest request) {
        return response(request, HttpStatus.UNPROCESSABLE_ENTITY, "UNKNOWN_APPLIANCE_TYPE",
                "선택한 가전제품 종류를 사용할 수 없습니다.", List.of());
    }

    @ExceptionHandler(InvalidInstallationPageException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidInstallationPage(HttpServletRequest request) {
        return response(request, HttpStatus.BAD_REQUEST, "INVALID_PAGE",
                "페이지 번호 또는 페이지 크기가 올바르지 않습니다.", List.of());
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleMemberNotFound(HttpServletRequest request) {
        return response(request, HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND",
                "회원을 찾을 수 없습니다.", List.of());
    }

    @ExceptionHandler(InvalidMemberRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidMemberRequest(HttpServletRequest request) {
        return response(request, HttpStatus.BAD_REQUEST, "INVALID_MEMBER_REQUEST",
                "회원 정보 요청 값이 올바르지 않습니다.", List.of());
    }

    @ExceptionHandler(AccountManagementConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountManagementConflict(HttpServletRequest request) {
        return response(request, HttpStatus.CONFLICT, "ACCOUNT_MANAGEMENT_CONFLICT",
                "관리자는 본인의 역할 또는 계정 상태를 변경할 수 없습니다.", List.of());
    }

    @ExceptionHandler(TechnicianProfileUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleTechnicianProfileUnavailable(HttpServletRequest request) {
        return response(request, HttpStatus.FORBIDDEN, "TECHNICIAN_PROFILE_UNAVAILABLE",
                "승인된 활성 기사 프로필을 찾을 수 없습니다.", List.of());
    }

    @ExceptionHandler(TechnicianProfileNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTechnicianProfileNotFound(HttpServletRequest request) {
        return response(request, HttpStatus.NOT_FOUND, "TECHNICIAN_PROFILE_NOT_FOUND",
                "기사 프로필을 찾을 수 없습니다.", List.of());
    }

    @ExceptionHandler(InvalidTechnicianProfileException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidTechnicianProfile(HttpServletRequest request) {
        return response(request, HttpStatus.BAD_REQUEST, "INVALID_TECHNICIAN_PROFILE",
                "기사 프로필의 품목, 지역 또는 가능 일정이 올바르지 않습니다.", List.of());
    }

    @ExceptionHandler(InvalidTechnicianAssignmentException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidTechnicianAssignment(HttpServletRequest request) {
        return response(request, HttpStatus.CONFLICT, "INVALID_TECHNICIAN_ASSIGNMENT",
                "예약 상태 또는 기사 적격 조건을 확인해 주세요.", List.of());
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleReservationNotFound(HttpServletRequest request) {
        return response(request, HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND",
                "예약을 찾을 수 없습니다.", List.of());
    }

    @ExceptionHandler(com.platform.technician.ReservationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTechnicianReservationNotFound(HttpServletRequest request) {
        return response(request, HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND",
                "예약을 찾을 수 없습니다.", List.of());
    }

    @ExceptionHandler(InvalidReservationException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidReservation(HttpServletRequest request) {
        return response(request, HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_RESERVATION_INTERVAL",
                "예약 시작·종료 시각 또는 멱등 키가 올바르지 않습니다.", List.of());
    }

    @ExceptionHandler(InvalidReservationPageException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidReservationPage(HttpServletRequest request) {
        return response(request, HttpStatus.BAD_REQUEST, "INVALID_PAGE",
                "페이지 번호 또는 페이지 크기가 올바르지 않습니다.", List.of());
    }

    @ExceptionHandler(ReservationNotAcceptableException.class)
    public ResponseEntity<ApiErrorResponse> handleReservationNotAcceptable(HttpServletRequest request) {
        return response(request, HttpStatus.CONFLICT, "RESERVATION_NOT_ACCEPTABLE",
                "현재 요청 또는 예약 상태에서는 해당 작업을 수행할 수 없습니다.", List.of());
    }

    @ExceptionHandler(ActiveReservationExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleActiveReservationExists(HttpServletRequest request) {
        return response(request, HttpStatus.CONFLICT, "ACTIVE_RESERVATION_EXISTS",
                "설치 요청에 이미 활성 예약이 있습니다.", List.of());
    }

    @ExceptionHandler(ReservationConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleReservationConflict(HttpServletRequest request) {
        return response(request, HttpStatus.CONFLICT, "RESERVATION_CONFLICT",
                "예약이 다른 요청과 충돌합니다. 최신 상태를 확인해 주세요.", List.of());
    }

    @ExceptionHandler(IdempotencyKeyConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleIdempotencyKeyConflict(HttpServletRequest request) {
        return response(request, HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT",
                "같은 멱등 키를 다른 예약 요청에 사용할 수 없습니다.", List.of());
    }

    @ExceptionHandler(InvalidTechnicianEvidenceException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidTechnicianEvidence(HttpServletRequest request) {
        return response(request, HttpStatus.BAD_REQUEST, "INVALID_TECHNICIAN_EVIDENCE",
                "작업 증빙은 작업 시작 후 JPEG, PNG 또는 WebP 이미지(10MB 이하)만 등록할 수 있습니다.", List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleUploadTooLarge(HttpServletRequest request) {
        return response(request, HttpStatus.PAYLOAD_TOO_LARGE, "UPLOAD_TOO_LARGE",
                "업로드 파일은 10MB 이하여야 합니다.", List.of());
    }

    @ExceptionHandler(TechnicianEvidenceStorageException.class)
    public ResponseEntity<ApiErrorResponse> handleTechnicianEvidenceStorage(
            TechnicianEvidenceStorageException exception,
            HttpServletRequest request
    ) {
        LOGGER.error("Unable to access technician evidence storage", exception);
        return response(request, HttpStatus.INTERNAL_SERVER_ERROR, "TECHNICIAN_EVIDENCE_STORAGE_ERROR",
                "작업 증빙 파일을 저장하거나 읽지 못했습니다.", List.of());
    }

    @ExceptionHandler(AssignmentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAssignmentNotFound(HttpServletRequest request) {
        return response(request, HttpStatus.NOT_FOUND, "ASSIGNMENT_NOT_FOUND",
                "배정된 작업을 찾을 수 없습니다.", List.of());
    }

    @ExceptionHandler(InvalidAssignmentPageException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidAssignmentPage(HttpServletRequest request) {
        return response(request, HttpStatus.BAD_REQUEST, "INVALID_PAGE",
                "페이지 번호 또는 페이지 크기가 올바르지 않습니다.", List.of());
    }

    @ExceptionHandler(InvalidAssignmentTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidAssignmentTransition(HttpServletRequest request) {
        return response(request, HttpStatus.CONFLICT, "INVALID_ASSIGNMENT_TRANSITION",
                "현재 작업 상태에서는 요청한 변경을 처리할 수 없습니다.", List.of());
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
