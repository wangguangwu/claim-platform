package com.claim.platform.common.tenant;

/**
 * Trusted tenant identity established by the edge layer, never by business request fields.
 *
 * @author wangguangwu
 */
public record TenantContext(String tenantCode) {
    public TenantContext {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new IllegalArgumentException("tenantCode must not be blank");
        }
    }
}
