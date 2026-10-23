package com.claim.platform.sign.domain.model;

/**
 * 定义理赔签收限界上下文中签收申请的合法状态。
 *
 * @author wangguangwu
 */
public enum SignStatus {
    IMAGE_NOT_UPLOAD,
    IMAGE_UPLOADED,
    NO_INSURED_INFO,
    SYNC_AUDIT,
    SYNC_AUDIT_SUCCESS,
    ISSUE_CASE,
    CASE_REVOKE
}
