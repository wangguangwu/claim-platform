package com.claim.platform.application.usecase.command;

/**
 * 表示从已鉴别租户接收签收申请的应用命令；请求体不携带租户字段。
 *
 * @author wangguangwu
 */
public record SubmitSignApplicationCommand(String tenantCode, String traceId, String externalApplyNo,
                                           String applicantName, String applicantInfo,
                                           String accidentPersonInfo, String mainInsuredInfo, String payeeInfo,
                                           String extJson) {
}
