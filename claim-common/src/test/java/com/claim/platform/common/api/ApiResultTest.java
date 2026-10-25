package com.claim.platform.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Tests the public response-envelope invariants. @author wangguangwu */
class ApiResultTest {
    @Test
    void shouldHidePayloadWhenFailureIsCreated() {
        ApiResult<String> result = ApiResult.failure("UNAUTHORIZED", "身份无效", "trace-1");
        assertThat(result.success()).isFalse();
        assertThat(result.data()).isNull();
        assertThat(result.traceId()).isEqualTo("trace-1");
    }
}
