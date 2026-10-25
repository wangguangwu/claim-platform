package com.claim.platform.sign.domain.caserelation.model;

/** Captures the Sign-to-case relation without importing case-domain facts into the Sign context.
 *
 * @author wangguangwu
 */
public record CaseRelation(String tenantCode, String signNo, String caseNo, String externalCaseNo) {
    public CaseRelation {
        if (tenantCode == null || tenantCode.isBlank() || signNo == null || signNo.isBlank()
                || caseNo == null || caseNo.isBlank()) {
            throw new IllegalArgumentException("tenantCode, signNo and caseNo must not be blank");
        }
    }
}
