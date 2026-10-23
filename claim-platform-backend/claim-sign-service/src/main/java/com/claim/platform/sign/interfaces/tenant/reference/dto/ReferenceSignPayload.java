package com.claim.platform.sign.interfaces.tenant.reference.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * reference 参考协议的签收报文，仅用于打通入口和验证协议适配机制，不代表真实租户。
 * {@code tenantCode} 与 {@code protocolVersion} 仅用于验证 payload 字段不参与路由，转换层不会读取。
 *
 * @author wangguangwu
 */
public record ReferenceSignPayload(@NotBlank String externalApplyNo,
                                     @NotNull @Valid ReferenceApplicantPayload applicant,
                                     @NotBlank String accidentDate,
                                     String tenantCode,
                                     String protocolVersion,
                                     Map<String, String> extension) {
}
