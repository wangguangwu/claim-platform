package com.claim.platform.sign.interfaces.tenant.core.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.claim.platform.sign.interfaces.tenant.core.exception.IntakePayloadParseException;
import com.claim.platform.sign.interfaces.tenant.support.SupportTenantIntakePayload;
import com.claim.platform.sign.interfaces.tenant.support.SupportTenantTestData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * 验证默认 JSON 解析器的解析、严格字段策略和敏感信息保护。
 *
 * @author wangguangwu
 */
class DefaultJsonParserTest {

    private final DefaultJsonParser<SupportTenantIntakePayload> parser =
            new DefaultJsonParser<>(new ObjectMapper(), SupportTenantIntakePayload.class);

    @Test
    void shouldParseValidJsonIntoTenantDto() {
        SupportTenantIntakePayload payload = parser.parse(SupportTenantTestData.VALID_PAYLOAD);

        assertThat(payload.externalApplyNo()).isEqualTo("EXT-001");
        assertThat(payload.accidentDate()).isEqualTo("2026-10-01");
        assertThat(payload.applicant().certificateType()).isEqualTo("IDCARD");
        assertThat(payload.extension()).containsEntry("channel", "APP");
    }

    @Test
    void shouldRejectMalformedJsonWithoutLeakingSensitiveValues() {
        String malformed = """
                {"externalApplyNo":"EXT-001","applicant":{"certificateNo":"%s","mobile":"%s"
                """.formatted(SupportTenantTestData.CERTIFICATE_NO, SupportTenantTestData.MOBILE);

        IntakePayloadParseException exception = catchThrowableOfType(
                () -> parser.parse(malformed), IntakePayloadParseException.class);

        assertThat(exception.fieldPath()).isEqualTo("applicant");
        assertThat(exception.getMessage())
                .doesNotContain(SupportTenantTestData.CERTIFICATE_NO)
                .doesNotContain(SupportTenantTestData.MOBILE);
        assertThat(exception.getCause()).isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void shouldRejectUnknownField() {
        String payload = SupportTenantTestData.VALID_PAYLOAD.replace(
                "\"externalApplyNo\": \"EXT-001\",",
                "\"externalApplyNo\": \"EXT-001\",\n  \"unknownField\": \"value\",");

        IntakePayloadParseException exception = catchThrowableOfType(
                () -> parser.parse(payload), IntakePayloadParseException.class);

        assertThat(exception.getMessage()).contains("报文解析失败");
        assertThat(exception.getCause()).isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void shouldReportFieldPathWhenValueTypeMismatch() {
        String payload = SupportTenantTestData.VALID_PAYLOAD.replace(
                "\"externalApplyNo\": \"EXT-001\"", "\"externalApplyNo\": {\"bad\": true}");

        IntakePayloadParseException exception = catchThrowableOfType(
                () -> parser.parse(payload), IntakePayloadParseException.class);

        assertThat(exception.fieldPath()).isEqualTo("externalApplyNo");
    }

    @Test
    void shouldRejectEmptyPayload() {
        IntakePayloadParseException exception = catchThrowableOfType(
                () -> parser.parse("  "), IntakePayloadParseException.class);

        assertThat(exception.fieldPath()).isEqualTo("payload");
    }

    @Test
    void shouldNotLogSensitiveValuesWhenParsingFails() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        rootLogger.addAppender(appender);
        try {
            String malformed = """
                    {"applicant":{"certificateNo":"%s","mobile":"%s"
                    """.formatted(SupportTenantTestData.CERTIFICATE_NO, SupportTenantTestData.MOBILE);
            catchThrowableOfType(() -> parser.parse(malformed), IntakePayloadParseException.class);
        } finally {
            rootLogger.detachAppender(appender);
        }

        assertThat(appender.list)
                .allSatisfy(event -> assertThat(event.getFormattedMessage())
                        .doesNotContain(SupportTenantTestData.CERTIFICATE_NO)
                        .doesNotContain(SupportTenantTestData.MOBILE));
    }
}
