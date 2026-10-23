package com.claim.platform.sign.interfaces.tenant.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.claim.platform.sign.application.command.SignCommand;
import com.claim.platform.sign.interfaces.tenant.core.context.TenantContext;
import com.claim.platform.sign.interfaces.tenant.core.context.TenantContextHolder;
import com.claim.platform.sign.interfaces.tenant.core.exception.SignCodeMappingException;
import com.claim.platform.sign.interfaces.tenant.core.exception.SignPayloadParseException;
import com.claim.platform.sign.interfaces.tenant.core.exception.SignRequestValidationException;
import com.claim.platform.sign.interfaces.tenant.core.pipeline.DefaultSignConvertPipeline;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolAdapterRegistry;
import com.claim.platform.sign.interfaces.tenant.reference.dto.ReferenceSignPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ReferenceTenantAdapterTest {

    private static final String CERTIFICATE_NO = "110101199001011234";
    private static final String MOBILE = "13800138000";
    private static final TenantContext CONTEXT = new TenantContext("reference", "v1", "trace-001");
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static final String VALID_PAYLOAD = """
            {
              "externalApplyNo": "EXT-001",
              "tenantCode": "forged-tenant",
              "protocolVersion": "v9",
              "accidentDate": "2026-10-01",
              "applicant": {
                "name": "张三",
                "certificateType": "IDCARD",
                "certificateNo": "%s",
                "mobile": "%s",
                "gender": "M",
                "birthDate": "1990-01-01",
                "relationshipToInsured": "SELF"
              },
              "extension": {
                "channel": "APP"
              }
            }
            """.formatted(CERTIFICATE_NO, MOBILE);

    private final ReferenceProtocolAdapter handler = new ReferenceProtocolAdapter(new ObjectMapper(), VALIDATOR);
    private final DefaultSignConvertPipeline pipeline = new DefaultSignConvertPipeline(
            new TenantProtocolAdapterRegistry(List.of(handler)));

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldConvertReferencePayload() {
        ReferenceSignPayload request = handler.parser().parse(VALID_PAYLOAD);
        handler.validator().validate(request);

        SignCommand command = handler.converter().convert(request, CONTEXT);

        assertThat(command.tenantCode()).isEqualTo("reference");
        assertThat(command.traceId()).isEqualTo("trace-001");
        assertThat(command.externalApplyNo()).isEqualTo("EXT-001");
        assertThat(command.applicantName()).isEqualTo("张三");
        assertThat(command.applicant().certificateType()).isEqualTo("ID_CARD");
        assertThat(command.applicant().certificateNo()).isEqualTo(CERTIFICATE_NO);
        assertThat(command.applicant().mobile()).isEqualTo(MOBILE);
        assertThat(command.applicant().relationshipToInsured()).isEqualTo("SELF");
        assertThat(command.extJson())
                .contains("\"accidentDate\":\"2026-10-01\"")
                .contains("\"channel\":\"APP\"")
                .doesNotContain("tenantCode")
                .doesNotContain("protocolVersion");
        assertThat(command.accidentPerson()).isNull();
        assertThat(command.mainInsured()).isNull();
        assertThat(command.payee()).isNull();
        assertThat(command.materials()).isEmpty();
    }

    @Test
    void shouldUseTrustedContextAndIgnorePayloadTenantFields() {
        SignCommand command = execute(VALID_PAYLOAD);

        assertThat(command.tenantCode()).isEqualTo("reference");
        assertThat(command.traceId()).isEqualTo("trace-001");
        assertThat(command.applicant().certificateType()).isEqualTo("ID_CARD");
    }

    @Test
    void shouldRejectUnknownCertificateCode() {
        String payload = VALID_PAYLOAD.replace("\"certificateType\": \"IDCARD\"",
                "\"certificateType\": \"UNKNOWN\"");

        SignCodeMappingException exception = catchThrowableOfType(
                () -> execute(payload), SignCodeMappingException.class);

        assertThat(exception.fieldPath()).isEqualTo("applicant.certificateType");
        assertThat(exception.getMessage()).doesNotContain(CERTIFICATE_NO).doesNotContain(MOBILE);
    }

    @Test
    void shouldRejectMissingRelationship() {
        String payload = VALID_PAYLOAD.replace(",\n    \"relationshipToInsured\": \"SELF\"", "");

        SignRequestValidationException exception = catchThrowableOfType(
                () -> execute(payload), SignRequestValidationException.class);

        assertThat(exception.fieldPath()).isEqualTo("applicant.relationshipToInsured");
    }

    @Test
    void shouldRejectUnknownField() {
        String payload = VALID_PAYLOAD.replace("\"externalApplyNo\": \"EXT-001\",",
                "\"externalApplyNo\": \"EXT-001\",\n  \"unknownField\": \"value\",");

        SignPayloadParseException exception = catchThrowableOfType(
                () -> execute(payload), SignPayloadParseException.class);

        assertThat(exception.getMessage()).contains("报文解析失败");
    }

    @Test
    void shouldReturnEquivalentCommandsOnRepeatedConversion() {
        SignCommand first = execute(VALID_PAYLOAD);
        SignCommand second = execute(VALID_PAYLOAD);

        assertThat(second).isEqualTo(first);
    }

    private SignCommand execute(String payload) {
        TenantContextHolder.set(CONTEXT);
        try {
            return pipeline.execute(payload);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
