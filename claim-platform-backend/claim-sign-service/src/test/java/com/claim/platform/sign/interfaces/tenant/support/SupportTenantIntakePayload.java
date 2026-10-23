package com.claim.platform.sign.interfaces.tenant.support;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 仅用于验证转换层扩展机制的测试租户报文，不属于生产租户协议。
 * {@code tenantCode} 与 {@code protocolVersion} 字段故意保留，用于验证 payload 中的租户信息不会生效。
 *
 * @author wangguangwu
 */
public record SupportTenantIntakePayload(@NotBlank String externalApplyNo,
                                         @NotNull @Valid SupportTenantApplicantPayload applicant,
                                         @NotBlank String accidentDate, String tenantCode, String protocolVersion,
                                         Map<String, String> extension) {
}
