package com.claim.platform.intake.interfaces.tenant.core.registry;

import java.util.regex.Pattern;

/**
 * 租户协议组件包的唯一匹配键，按 {@code tenantCode + protocolVersion} 精确匹配。
 * 构造时校验格式，注册后不可变。
 *
 * @param tenantCode      租户编码
 * @param protocolVersion 协议版本
 * @author wangguangwu
 */
public record TenantProtocolKey(String tenantCode, String protocolVersion) {

    private static final Pattern TENANT_CODE_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final Pattern PROTOCOL_VERSION_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,32}");

    public TenantProtocolKey {
        if (tenantCode == null || !TENANT_CODE_PATTERN.matcher(tenantCode).matches()) {
            throw new IllegalArgumentException("tenantCode 格式非法");
        }
        if (protocolVersion == null || !PROTOCOL_VERSION_PATTERN.matcher(protocolVersion).matches()) {
            throw new IllegalArgumentException("protocolVersion 格式非法");
        }
    }
}
