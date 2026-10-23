package com.claim.platform.intake.interfaces.tenant.core.context;

/**
 * 已完成验签、解密和鉴权后的可信租户上下文快照，由入口适配器写入 {@link TenantContextHolder}。
 * 转换层只消费该快照，不得从 Raw Payload 推断租户、协议版本或 Trace。
 *
 * @param tenantCode      受信任租户编码
 * @param protocolVersion 受信任协议版本
 * @param traceId         链路追踪标识
 * @author wangguangwu
 */
public record TenantContext(String tenantCode, String protocolVersion, String traceId) {

    public TenantContext {
        requireText(tenantCode, "tenantCode");
        requireText(protocolVersion, "protocolVersion");
        requireText(traceId, "traceId");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
    }
}
