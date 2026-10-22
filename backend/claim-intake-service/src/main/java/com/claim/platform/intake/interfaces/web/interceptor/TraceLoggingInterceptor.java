package com.claim.platform.intake.interfaces.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Web 请求拦截器，在请求入口从可信头注入 {@code traceId} 与 {@code tenantCode} 到 SLF4J MDC，
 * 使整条链路日志可通过这两个维度关联；请求完成后清理，避免线程复用导致上下文泄漏。
 * 头来源与 {@link GatewayRequestContextResolver} 一致，仅用于日志关联，不参与鉴权。
 *
 * @author wangguangwu
 */
@Component
public class TraceLoggingInterceptor implements HandlerInterceptor {

    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_TENANT_CODE = "tenantCode";

    /**
     * 在处理器执行前注入 MDC；缺省或空白头不写入对应维度。
     *
     * @return 始终返回 true，不阻断请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId != null && !traceId.isBlank()) {
            MDC.put(MDC_TRACE_ID, traceId);
        }
        String tenantCode = request.getHeader("X-Claim-Tenant");
        if (tenantCode != null && !tenantCode.isBlank()) {
            MDC.put(MDC_TENANT_CODE, tenantCode);
        }
        return true;
    }

    /**
     * 请求完成后清理 MDC，防止线程池复用导致上下文串号。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove(MDC_TRACE_ID);
        MDC.remove(MDC_TENANT_CODE);
    }
}
