package com.claim.platform.intake.adapter.web;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 定义接收签收申请的 HTTP 请求体；租户只允许来自可信请求头。
 *
 * @author wangguangwu
 */
public record CreateIntakeRequest(
        @NotBlank @Size(max = 64) String externalApplyNo,
        @Size(max = 64) String applicantName,
        JsonNode applicantInfo,
        JsonNode accidentPersonInfo,
        JsonNode mainInsuredInfo,
        JsonNode payeeInfo,
        JsonNode extJson) {
}
