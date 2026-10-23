package com.claim.platform.sign.interfaces.web.advice;

import static org.assertj.core.api.Assertions.assertThat;

import com.claim.platform.common.web.ApiResult;
import com.claim.platform.sign.application.exception.SignNoSequenceExhaustedException;
import com.claim.platform.sign.application.exception.SignNoUnavailableException;
import com.claim.platform.sign.application.exception.SignNoUnavailableReason;
import com.claim.platform.sign.interfaces.web.context.GatewayRequestContextResolver;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * 验证签收号不可用和耗尽的 HTTP 映射、重试提示和响应脱敏。
 *
 * @author wangguangwu
 */
class SignNoExceptionHandlerTest {

    private static final String TRACE_ID = "trace-sign-no";

    private final SignExceptionHandler handler = new SignExceptionHandler();

    @Test
    void shouldMapUnavailableToServiceUnavailableWithRetryAfter() {
        ResponseEntity<ApiResult<Void>> response = handler.signNoUnavailable(
                new SignNoUnavailableException(SignNoUnavailableReason.REDIS_UNAVAILABLE), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("5");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().code()).isEqualTo("INTAKE_NUMBER_UNAVAILABLE");
        assertThat(response.getBody().message()).isEqualTo("签收号服务暂不可用，请稍后重试");
        assertThat(response.getBody().traceId()).isEqualTo(TRACE_ID);
        assertThat(response.getBody().message()).doesNotContain("REDIS_UNAVAILABLE");
    }

    @Test
    void shouldMapExhaustedToServiceUnavailableWithoutRetryAfter() {
        ResponseEntity<ApiResult<Void>> response = handler.signNoExhausted(
                new SignNoSequenceExhaustedException(), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTAKE_NUMBER_EXHAUSTED");
        assertThat(response.getBody().message()).isEqualTo("今日签收号已用尽，请联系平台");
        assertThat(response.getBody().traceId()).isEqualTo(TRACE_ID);
    }

    @Test
    void shouldGenerateTraceIdWhenGatewayAttributeMissing() {
        ResponseEntity<ApiResult<Void>> response = handler.signNoUnavailable(
                new SignNoUnavailableException(SignNoUnavailableReason.RETRY_EXHAUSTED),
                new MockHttpServletRequest());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().traceId()).isNotBlank();
    }

    @Test
    void shouldKeepNonUniqueIntegrityFailureAsInternalError() {
        ResponseEntity<ApiResult<Void>> response = handler.unexpected(
                new DataIntegrityViolationException("check constraint failed"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("系统繁忙，请稍后重试");
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(GatewayRequestContextResolver.TRACE_ATTRIBUTE, TRACE_ID);
        return request;
    }
}
