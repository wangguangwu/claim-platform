package com.claim.platform.sign.domain.model;

/**
 * 定义 Claim Intake 限界上下文中签收申请的合法状态。
 *
 * @author wangguangwu
 */
public enum IntakeStatus {
    IMAGE_NOT_UPLOAD,
    IMAGE_UPLOADED,
    NO_INSURED_INFO,
    SYNC_AUDIT,
    SYNC_AUDIT_SUCCESS,
    ISSUE_CASE,
    CASE_REVOKE
}
