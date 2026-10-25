package com.claim.platform.common.trace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Tests trace generation and cleanup. @author wangguangwu */
class TraceContextTest {
    @AfterEach void cleanup() { TraceContext.clear(); }

    @Test
    void shouldReuseBoundTraceId() {
        TraceContext.bind("trace-1");
        assertThat(TraceContext.currentOrCreate()).isEqualTo("trace-1");
    }
}
