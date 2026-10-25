package com.claim.platform.common.trace;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Verifies servlet entry-point trace propagation and request cleanup. @author wangguangwu */
class TraceServletFilterTest {
    private final TraceServletFilter filter = new TraceServletFilter();

    @AfterEach
    void cleanup() {
        TraceContext.clear();
        MDC.clear();
    }

    @Test
    void shouldForwardTraceIdToResponseAndMdcDuringRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceContext.TRACE_ID_HEADER, "trace-forwarded");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (requestValue, responseValue) -> {
            assertThat(TraceContext.current()).contains("trace-forwarded");
            assertThat(MDC.get(TraceContext.TRACE_ID_MDC_KEY)).isEqualTo("trace-forwarded");
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(TraceContext.TRACE_ID_HEADER)).isEqualTo("trace-forwarded");
        assertThat(TraceContext.current()).isEmpty();
        assertThat(MDC.get(TraceContext.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    void shouldGenerateTraceIdWhenRequestDoesNotProvideOne() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, (requestValue, responseValue) -> { });

        assertThat(response.getHeader(TraceContext.TRACE_ID_HEADER)).hasSize(32);
    }
}
