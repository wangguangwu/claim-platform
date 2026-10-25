package com.claim.platform.sign.config;

import com.claim.platform.common.trace.TraceServletFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Installs platform technical entry-point behavior without adding Sign domain behavior.
 *
 * @author wangguangwu
 */
@Configuration
public class SignServiceConfiguration {
    /** Registers a trace filter for all incoming local and gateway-forwarded requests. */
    @Bean
    public TraceServletFilter traceServletFilter() {
        return new TraceServletFilter();
    }
}
