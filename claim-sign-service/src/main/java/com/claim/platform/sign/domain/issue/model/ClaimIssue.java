package com.claim.platform.sign.domain.issue.model;

/** Isolates one platform-issued problem record identified within a tenant Sign scope.
 *
 * @author wangguangwu
 */
public record ClaimIssue(String tenantCode, String signNo, String issueNo, String auditor, String reason,
                         String extensionJson, boolean syncedToTenant, boolean requiresSupplement) {
    public ClaimIssue {
        require(tenantCode, "tenantCode");
        require(signNo, "signNo");
        require(issueNo, "issueNo");
        require(reason, "reason");
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
