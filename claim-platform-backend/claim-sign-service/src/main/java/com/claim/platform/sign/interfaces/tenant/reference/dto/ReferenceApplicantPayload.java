package com.claim.platform.sign.interfaces.tenant.reference.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * reference 参考协议的申请人报文，仅用于打通入口和验证协议适配机制。
 *
 * @author wangguangwu
 */
public record ReferenceApplicantPayload(@NotBlank String name,
                                        @NotBlank String certificateType,
                                        @NotBlank String certificateNo,
                                        @NotBlank String mobile,
                                        @NotBlank String gender,
                                        @NotBlank String birthDate,
                                        String relationshipToInsured) {
}
