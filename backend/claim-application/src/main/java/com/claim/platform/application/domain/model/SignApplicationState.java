package com.claim.platform.application.domain.model;

/**
 * 表示从持久化层恢复签收申请聚合所需的完整领域状态，
 * 不包含数据库主键、乐观锁版本等基础设施属性。
 *
 * @author wangguangwu
 */
public record SignApplicationState(String tenantCode, String signNo, String externalApplyNo, String applicantName,
                                   String applicantInfo, String accidentPersonInfo, String mainInsuredInfo,
                                   String payeeInfo, String extJson, SignApplicationStatus status) {
}
