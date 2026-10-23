package com.claim.platform.sign.interfaces.tenant.core.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.claim.platform.sign.application.command.SignCommand;
import com.claim.platform.sign.domain.model.person.Applicant;
import com.claim.platform.sign.interfaces.tenant.core.context.TenantContext;
import com.claim.platform.sign.interfaces.tenant.core.context.TenantContextHolder;
import com.claim.platform.sign.interfaces.tenant.core.converter.RequestConverter;
import com.claim.platform.sign.interfaces.tenant.core.exception.SignCodeMappingException;
import com.claim.platform.sign.interfaces.tenant.core.exception.SignPayloadParseException;
import com.claim.platform.sign.interfaces.tenant.core.exception.SignRequestValidationException;
import com.claim.platform.sign.interfaces.tenant.core.exception.TenantProtocolNotSupportedException;
import com.claim.platform.sign.interfaces.tenant.core.parser.RequestParser;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolAdapter;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolKey;
import com.claim.platform.sign.interfaces.tenant.core.validator.RequestValidator;
import com.claim.platform.sign.interfaces.tenant.support.StubTenantProtocolAdapter;
import com.claim.platform.sign.interfaces.tenant.support.SupportTenantSignPayload;
import com.claim.platform.sign.interfaces.tenant.support.SupportTenantProtocols;
import com.claim.platform.sign.interfaces.tenant.support.SupportTenantTestData;
import com.claim.platform.sign.interfaces.web.exception.TenantContextMissingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/**
 * 验证固定转换流程、可信上下文、完整命令和失败终止语义。
 *
 * @author wangguangwu
 */
@ExtendWith(MockitoExtension.class)
class DefaultSignConvertPipelineTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldExecuteParserValidatorConverterInFixedOrder() {
        TenantContext context = SupportTenantTestData.context();
        TenantContextHolder.set(context);
        SupportTenantSignPayload payload = new SupportTenantSignPayload(
                "EXT-001", null, "2026-10-01", null, null, null);
        SignCommand command = new SignCommand(SupportTenantTestData.TENANT_CODE,
                SupportTenantTestData.TRACE_ID, "EXT-001", "张三", null, null, null, null, List.of(), null);
        RequestParser<SupportTenantSignPayload> parser = mock(RequestParser.class);
        RequestValidator<SupportTenantSignPayload> validator = mock(RequestValidator.class);
        RequestConverter<SupportTenantSignPayload> converter = mock(RequestConverter.class);
        given(parser.parse(SupportTenantTestData.VALID_PAYLOAD)).willReturn(payload);
        given(converter.convert(payload, context)).willReturn(command);
        DefaultSignConvertPipeline pipeline = pipeline(new StubTenantProtocolAdapter<>(
                protocolKey(), parser, validator, converter));

        SignCommand result = pipeline.execute(SupportTenantTestData.VALID_PAYLOAD);

        assertThat(result).isSameAs(command);
        InOrder inOrder = inOrder(parser, validator, converter);
        inOrder.verify(parser).parse(SupportTenantTestData.VALID_PAYLOAD);
        inOrder.verify(validator).validate(payload);
        inOrder.verify(converter).convert(payload, context);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldStopWhenParserFails() {
        TenantContextHolder.set(SupportTenantTestData.context());
        RequestParser<SupportTenantSignPayload> parser = mock(RequestParser.class);
        RequestValidator<SupportTenantSignPayload> validator = mock(RequestValidator.class);
        RequestConverter<SupportTenantSignPayload> converter = mock(RequestConverter.class);
        SignPayloadParseException failure = new SignPayloadParseException("payload");
        given(parser.parse(SupportTenantTestData.VALID_PAYLOAD)).willThrow(failure);
        DefaultSignConvertPipeline pipeline = pipeline(new StubTenantProtocolAdapter<>(
                protocolKey(), parser, validator, converter));

        assertThatThrownBy(() -> pipeline.execute(SupportTenantTestData.VALID_PAYLOAD)).isSameAs(failure);
        verifyNoInteractions(validator, converter);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldStopWhenValidationFails() {
        TenantContextHolder.set(SupportTenantTestData.context());
        SupportTenantSignPayload payload = new SupportTenantSignPayload(
                "EXT-001", null, "2026-10-01", null, null, null);
        RequestParser<SupportTenantSignPayload> parser = mock(RequestParser.class);
        RequestValidator<SupportTenantSignPayload> validator = mock(RequestValidator.class);
        RequestConverter<SupportTenantSignPayload> converter = mock(RequestConverter.class);
        SignRequestValidationException failure = new SignRequestValidationException("externalApplyNo");
        given(parser.parse(SupportTenantTestData.VALID_PAYLOAD)).willReturn(payload);
        willThrow(failure).given(validator).validate(payload);
        DefaultSignConvertPipeline pipeline = pipeline(new StubTenantProtocolAdapter<>(
                protocolKey(), parser, validator, converter));

        assertThatThrownBy(() -> pipeline.execute(SupportTenantTestData.VALID_PAYLOAD)).isSameAs(failure);
        verifyNoInteractions(converter);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRejectWhenTenantContextMissing() {
        RequestParser<SupportTenantSignPayload> parser = mock(RequestParser.class);
        RequestValidator<SupportTenantSignPayload> validator = mock(RequestValidator.class);
        RequestConverter<SupportTenantSignPayload> converter = mock(RequestConverter.class);
        DefaultSignConvertPipeline pipeline = pipeline(new StubTenantProtocolAdapter<>(
                protocolKey(), parser, validator, converter));

        assertThatThrownBy(() -> pipeline.execute(SupportTenantTestData.VALID_PAYLOAD))
                .isInstanceOf(TenantContextMissingException.class);
        verifyNoInteractions(parser, validator, converter);
    }

    @Test
    void shouldUseTrustedTenantContextInsteadOfPayloadFields() {
        TenantContextHolder.set(SupportTenantTestData.context());
        DefaultSignConvertPipeline pipeline = pipeline(SupportTenantProtocols.v1(objectMapper));

        SignCommand command = pipeline.execute(SupportTenantTestData.VALID_PAYLOAD);

        assertThat(command.tenantCode()).isEqualTo(SupportTenantTestData.TENANT_CODE);
        assertThat(command.traceId()).isEqualTo(SupportTenantTestData.TRACE_ID);
        assertThat(command.extJson()).doesNotContain("forged-tenant").doesNotContain("v9");
    }

    @Test
    void shouldReturnCompletePlatformCommand() {
        TenantContextHolder.set(SupportTenantTestData.context());
        DefaultSignConvertPipeline pipeline = pipeline(SupportTenantProtocols.v1(objectMapper));

        SignCommand command = pipeline.execute(SupportTenantTestData.VALID_PAYLOAD);

        assertThat(command.externalApplyNo()).isEqualTo("EXT-001");
        assertThat(command.applicantName()).isEqualTo("张三");
        assertThat(command.applicant()).isInstanceOf(Applicant.class);
        assertThat(command.applicant().certificateType()).isEqualTo("ID_CARD");
        assertThat(command.applicant().certificateNo()).isEqualTo(SupportTenantTestData.CERTIFICATE_NO);
        assertThat(command.extJson())
                .contains("\"accidentDate\":\"2026-10-01\"")
                .contains("\"channel\":\"APP\"")
                .doesNotContain("certificateNo")
                .doesNotContain("mobile");
        assertThat(Arrays.stream(SignCommand.class.getRecordComponents())
                .map(RecordComponent::getType))
                .noneMatch(JsonNode.class::isAssignableFrom);
    }

    @Test
    void shouldApplyTenantSpecificRuleAfterPublicRules() {
        TenantContextHolder.set(SupportTenantTestData.context());
        DefaultSignConvertPipeline pipeline = pipeline(SupportTenantProtocols.v1(objectMapper));

        SignRequestValidationException exception = catchThrowableOfType(
                () -> pipeline.execute(SupportTenantTestData.payloadWithoutRelationship()),
                SignRequestValidationException.class);

        assertThat(exception.fieldPath()).isEqualTo("applicant.relationshipToInsured");
    }

    @Test
    void shouldFailWithFieldPathWhenCertificateCodeUnknown() {
        TenantContextHolder.set(SupportTenantTestData.context());
        DefaultSignConvertPipeline pipeline = pipeline(SupportTenantProtocols.v1(objectMapper));

        SignCodeMappingException exception = catchThrowableOfType(
                () -> pipeline.execute(SupportTenantTestData.payloadWithCertificateType("UNKNOWN")),
                SignCodeMappingException.class);

        assertThat(exception.fieldPath()).isEqualTo("applicant.certificateType");
        assertThat(exception.getMessage())
                .doesNotContain("UNKNOWN")
                .doesNotContain(SupportTenantTestData.CERTIFICATE_NO)
                .doesNotContain(SupportTenantTestData.MOBILE);
    }

    @Test
    void shouldReturnEquivalentCommandsOnRepeatedConversion() {
        TenantContextHolder.set(SupportTenantTestData.context());
        DefaultSignConvertPipeline pipeline = pipeline(SupportTenantProtocols.v1(objectMapper));

        SignCommand first = pipeline.execute(SupportTenantTestData.VALID_PAYLOAD);
        SignCommand second = pipeline.execute(SupportTenantTestData.VALID_PAYLOAD);

        assertThat(first).isEqualTo(second);
        assertThat(Arrays.stream(SignCommand.class.getRecordComponents())
                .map(RecordComponent::getName))
                .doesNotContain("signNo");
    }

    @Test
    void shouldRejectUnregisteredProtocolWithoutFallback() {
        TenantContextHolder.set(new TenantContext("tenant-b", SupportTenantTestData.PROTOCOL_VERSION,
                SupportTenantTestData.TRACE_ID));
        DefaultSignConvertPipeline pipeline = pipeline(SupportTenantProtocols.v1(objectMapper));

        assertThatThrownBy(() -> pipeline.execute(SupportTenantTestData.VALID_PAYLOAD))
                .isInstanceOf(TenantProtocolNotSupportedException.class);
    }

    @Test
    void shouldNotLogSensitiveValuesWhenConversionFails() {
        TenantContextHolder.set(SupportTenantTestData.context());
        DefaultSignConvertPipeline pipeline = pipeline(SupportTenantProtocols.v1(objectMapper));
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        rootLogger.addAppender(appender);
        try {
            catchThrowableOfType(
                    () -> pipeline.execute(SupportTenantTestData.payloadWithCertificateType("UNKNOWN")),
                    SignCodeMappingException.class);
        } finally {
            rootLogger.detachAppender(appender);
        }

        assertThat(appender.list.stream().map(ILoggingEvent::getFormattedMessage))
                .noneMatch(message -> message.contains(SupportTenantTestData.CERTIFICATE_NO)
                        || message.contains(SupportTenantTestData.MOBILE));
    }

    @SafeVarargs
    private final DefaultSignConvertPipeline pipeline(TenantProtocolAdapter<?>... handlers) {
        return new DefaultSignConvertPipeline(SupportTenantProtocols.registry(handlers));
    }

    private TenantProtocolKey protocolKey() {
        return new TenantProtocolKey(SupportTenantTestData.TENANT_CODE, SupportTenantTestData.PROTOCOL_VERSION);
    }
}
