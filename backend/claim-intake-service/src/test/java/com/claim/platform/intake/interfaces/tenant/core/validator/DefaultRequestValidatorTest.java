package com.claim.platform.intake.interfaces.tenant.core.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.claim.platform.intake.interfaces.tenant.core.exception.IntakeRequestValidationException;
import com.claim.platform.intake.interfaces.tenant.core.parser.DefaultJsonParser;
import com.claim.platform.intake.interfaces.tenant.support.SupportTenantApplicantPayload;
import com.claim.platform.intake.interfaces.tenant.support.SupportTenantIntakePayload;
import com.claim.platform.intake.interfaces.tenant.support.SupportTenantTestData;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

/**
 * 验证默认校验器对公共必填、嵌套字段和空请求的处理。
 *
 * @author wangguangwu
 */
class DefaultRequestValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final DefaultRequestValidator<SupportTenantIntakePayload> requestValidator =
            new DefaultRequestValidator<>(validator);

    @Test
    void shouldAcceptPayloadThatSatisfiesPublicRules() {
        SupportTenantIntakePayload payload = parse(SupportTenantTestData.VALID_PAYLOAD);

        assertThatCode(() -> requestValidator.validate(payload)).doesNotThrowAnyException();
    }

    @Test
    void shouldReportMissingRequiredField() {
        SupportTenantIntakePayload payload = new SupportTenantIntakePayload(
                null, applicant(), "2026-10-01", null, null, null);

        IntakeRequestValidationException exception = catchThrowableOfType(
                () -> requestValidator.validate(payload), IntakeRequestValidationException.class);

        assertThat(exception.fieldPath()).isEqualTo("externalApplyNo");
    }

    @Test
    void shouldReportNestedFieldPath() {
        SupportTenantApplicantPayload applicant = new SupportTenantApplicantPayload(
                null, "IDCARD", SupportTenantTestData.CERTIFICATE_NO, SupportTenantTestData.MOBILE,
                "M", "1990-01-01", "SELF");
        SupportTenantIntakePayload payload = new SupportTenantIntakePayload(
                "EXT-001", applicant, "2026-10-01", null, null, null);

        IntakeRequestValidationException exception = catchThrowableOfType(
                () -> requestValidator.validate(payload), IntakeRequestValidationException.class);

        assertThat(exception.fieldPath()).isEqualTo("applicant.name");
    }

    @Test
    void shouldRejectNullRequest() {
        IntakeRequestValidationException exception = catchThrowableOfType(
                () -> requestValidator.validate(null), IntakeRequestValidationException.class);

        assertThat(exception.fieldPath()).isEqualTo("request");
    }

    private SupportTenantIntakePayload parse(String payload) {
        return new DefaultJsonParser<>(new ObjectMapper(), SupportTenantIntakePayload.class).parse(payload);
    }

    private SupportTenantApplicantPayload applicant() {
        return new SupportTenantApplicantPayload("张三", "IDCARD", SupportTenantTestData.CERTIFICATE_NO,
                SupportTenantTestData.MOBILE, "M", "1990-01-01", "SELF");
    }
}
