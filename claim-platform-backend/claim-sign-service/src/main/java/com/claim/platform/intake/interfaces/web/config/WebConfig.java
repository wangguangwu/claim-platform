package com.claim.platform.intake.interfaces.web.config;

import com.claim.platform.intake.interfaces.web.interceptor.TraceLoggingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册 Web 层拦截器；此处挂载 {@link TraceLoggingInterceptor} 以在请求边界注入与清理 MDC。
 *
 * @author wangguangwu
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TraceLoggingInterceptor traceLoggingInterceptor;

    public WebConfig(TraceLoggingInterceptor traceLoggingInterceptor) {
        this.traceLoggingInterceptor = traceLoggingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceLoggingInterceptor);
    }
}
