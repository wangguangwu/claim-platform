package com.claim.platform.sign.interfaces.web.request;

import com.claim.platform.sign.interfaces.web.ClaimIntakeWebContract;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 定义接收签收申请的 HTTP 请求体；租户只允许来自可信请求头。
 *
 * @author wangguangwu
 */
public record CreateIntakeRequest(
        @NotBlank @Size(max = ClaimIntakeWebContract.Limits.IDEMPOTENCY_KEY_LENGTH) String externalApplyNo,
        @Size(max = ClaimIntakeWebContract.Limits.APPLICANT_NAME_LENGTH) String applicantName,
        JsonNode applicantInfo,
        JsonNode accidentPersonInfo,
        JsonNode mainInsuredInfo,
        JsonNode payeeInfo,
        JsonNode extJson) {
}
