package com.claim.platform.sign.domain.model;

/**
 * 表示从持久化层恢复签收申请聚合所需的完整领域状态，不包含数据库主键、乐观锁版本等基础设施属性。
 * 人员明细以 {@code extJson} 承载，领域状态不设人员字段。
 *
 * @author wangguangwu
 */
public record SignState(String tenantCode, String signNo, String externalApplyNo, String applicantName,
                               String extJson, SignStatus status) {
}
