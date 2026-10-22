package com.claim.platform.intake.interfaces.web.context;

/**
 * 表示已经过 Gateway 鉴别的调用上下文，只可由 Web Adapter 构造。
 *
 * @author wangguangwu
 */
public record GatewayRequestContext(String tenantCode, String traceId) {
}
