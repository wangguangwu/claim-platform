package com.claim.platform.sign.interfaces.tenant.core.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.claim.platform.sign.interfaces.tenant.core.exception.SignRequestValidationException;
import com.claim.platform.sign.interfaces.tenant.core.parser.DefaultJsonParser;
import com.claim.platform.sign.interfaces.tenant.support.SupportTenantApplicantPayload;
import com.claim.platform.sign.interfaces.tenant.support.SupportTenantSignPayload;
import com.claim.platform.sign.interfaces.tenant.support.SupportTenantTestData;
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
    private final DefaultRequestValidator<SupportTenantSignPayload> requestValidator =
            new DefaultRequestValidator<>(validator);

    @Test
    void shouldAcceptPayloadThatSatisfiesPublicRules() {
        SupportTenantSignPayload payload = parse(SupportTenantTestData.VALID_PAYLOAD);

        assertThatCode(() -> requestValidator.validate(payload)).doesNotThrowAnyException();
    }

    @Test
    void shouldReportMissingRequiredField() {
        SupportTenantSignPayload payload = new SupportTenantSignPayload(
                null, applicant(), "2026-10-01", null, null, null);

        SignRequestValidationException exception = catchThrowableOfType(
                () -> requestValidator.validate(payload), SignRequestValidationException.class);

        assertThat(exception.fieldPath()).isEqualTo("externalApplyNo");
    }

    @Test
    void shouldReportNestedFieldPath() {
        SupportTenantApplicantPayload applicant = new SupportTenantApplicantPayload(
                null, "IDCARD", SupportTenantTestData.CERTIFICATE_NO, SupportTenantTestData.MOBILE,
                "M", "1990-01-01", "SELF");
        SupportTenantSignPayload payload = new SupportTenantSignPayload(
                "EXT-001", applicant, "2026-10-01", null, null, null);

        SignRequestValidationException exception = catchThrowableOfType(
                () -> requestValidator.validate(payload), SignRequestValidationException.class);

        assertThat(exception.fieldPath()).isEqualTo("applicant.name");
    }

    @Test
    void shouldRejectNullRequest() {
        SignRequestValidationException exception = catchThrowableOfType(
                () -> requestValidator.validate(null), SignRequestValidationException.class);

        assertThat(exception.fieldPath()).isEqualTo("request");
    }

    private SupportTenantSignPayload parse(String payload) {
        return new DefaultJsonParser<>(new ObjectMapper(), SupportTenantSignPayload.class).parse(payload);
    }

    private SupportTenantApplicantPayload applicant() {
        return new SupportTenantApplicantPayload("张三", "IDCARD", SupportTenantTestData.CERTIFICATE_NO,
                SupportTenantTestData.MOBILE, "M", "1990-01-01", "SELF");
    }
}
