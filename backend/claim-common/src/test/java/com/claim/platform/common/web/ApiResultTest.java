package com.claim.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResultTest {

    @Test
    void shouldCreateSuccessResponseWithStableFields() {
        ApiResult<String> result = ApiResult.success("application-001", "trace-001");

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo("OK");
        assertThat(result.message()).isEqualTo("操作成功");
        assertThat(result.traceId()).isEqualTo("trace-001");
        assertThat(result.data()).isEqualTo("application-001");
    }

    @Test
    void shouldCreateFailureResponseWithoutData() {
        ApiResult<Void> result = ApiResult.failure("REQUEST_VALIDATION_FAILED", "请求参数不合法", "trace-002");

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("REQUEST_VALIDATION_FAILED");
        assertThat(result.message()).isEqualTo("请求参数不合法");
        assertThat(result.traceId()).isEqualTo("trace-002");
        assertThat(result.data()).isNull();
    }
}
