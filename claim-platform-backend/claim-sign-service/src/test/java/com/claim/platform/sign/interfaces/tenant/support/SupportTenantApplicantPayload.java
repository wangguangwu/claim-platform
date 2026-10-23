package com.claim.platform.sign.interfaces.tenant.support;

import jakarta.validation.constraints.NotBlank;

/**
 * 测试租户的申请人报文，用于验证嵌套公共校验和租户差异校验。
 *
 * @author wangguangwu
 */
public record SupportTenantApplicantPayload(@NotBlank String name, @NotBlank String certificateType,
                                            @NotBlank String certificateNo, @NotBlank String mobile,
                                            @NotBlank String gender, @NotBlank String birthDate,
                                            String relationshipToInsured) {
}
