package com.claim.platform.sign.interfaces.web.request;

import com.claim.platform.sign.interfaces.web.SignWebContract;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 定义接收签收申请的 HTTP 请求体；租户只允许来自可信请求头。
 *
 * @author wangguangwu
 */
public record SignRequest(
        @NotBlank @Size(max = SignWebContract.Limits.IDEMPOTENCY_KEY_LENGTH) String externalApplyNo,
        @Size(max = SignWebContract.Limits.APPLICANT_NAME_LENGTH) String applicantName,
        JsonNode applicantInfo,
        JsonNode accidentPersonInfo,
        JsonNode mainInsuredInfo,
        JsonNode payeeInfo,
        JsonNode extJson) {
}
