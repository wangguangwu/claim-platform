package com.claim.platform.sign.application.result;

/** Is the tenant-scoped internal projection of a platform ClaimIssue.
 *
 * @author wangguangwu
 */
public record ClaimIssueView(String issueNo, String auditor, String reason, String extensionJson,
                             boolean syncedToTenant, boolean requiresSupplement) {
}
