package com.claim.platform.sign.domain.interactionlog.model;

/** Contains the platform-approved Sign interaction types and prevents synonym drift.
 *
 * @author wangguangwu
 */
public enum InteractionType {
    SIGN_SUBMIT,
    SIGN_QUERY,
    SIGN_REVOKE,
    MATERIAL_SUBMIT,
    MATERIAL_FETCH,
    INSURED_QUERY,
    AUTO_UNDERWRITING,
    AUDIT_SYNC,
    AUDIT_RESULT,
    AUDIT_ISSUE,
    ISSUE_NOTIFY,
    ISSUE_REPLY,
    CASE_COMPLETE
}
