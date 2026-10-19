package com.platform.installation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateInstallationRequestValidationTest {

    private static Validator validator;
    private static jakarta.validation.ValidatorFactory validatorFactory;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsCompleteFutureInstallationRequest() {
        CreateInstallationRequest request = new CreateInstallationRequest(
                "WASHING_MACHINE",
                "서울특별시 강남구 테헤란로 1",
                "경기도 성남시 분당구 판교로 2",
                OffsetDateTime.now().plusDays(2)
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsMissingFieldsAndPastDate() {
        CreateInstallationRequest request = new CreateInstallationRequest(
                " ",
                "",
                " ",
                OffsetDateTime.now().minusDays(1)
        );

        assertFalse(validator.validate(request).isEmpty());
    }
}
