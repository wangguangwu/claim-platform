package com.claim.platform.push.config;

import com.claim.platform.common.trace.TraceServletFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Installs shared technical request tracing without pre-empting Push domain implementation.
 *
 * @author wangguangwu
 */
@Configuration
public class PushServiceConfiguration {
    /** Registers a trace filter for all incoming local and gateway-forwarded requests. */
    @Bean
    public TraceServletFilter traceServletFilter() {
        return new TraceServletFilter();
    }
}
