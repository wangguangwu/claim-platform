package com.claim.platform.intake.interfaces.tenant.core.exception;

/**
 * 表示当前 {@code tenantCode + protocolVersion} 没有完整注册的转换组件。
 * 不允许回退到其他租户或其他协议版本。
 *
 * @author wangguangwu
 */
public class TenantProtocolNotSupportedException extends RuntimeException {

    private final String tenantCode;
    private final String protocolVersion;

    public TenantProtocolNotSupportedException(String tenantCode, String protocolVersion) {
        super("租户协议未注册，tenantCode=" + tenantCode + "，protocolVersion=" + protocolVersion);
        this.tenantCode = tenantCode;
        this.protocolVersion = protocolVersion;
    }

    public String tenantCode() {
        return tenantCode;
    }

    public String protocolVersion() {
        return protocolVersion;
    }
}
