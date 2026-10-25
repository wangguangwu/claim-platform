package com.claim.platform.sign.domain.issue.repository;

import com.claim.platform.sign.domain.issue.model.ClaimIssue;
import java.util.List;
import java.util.Optional;

/** Persists tenant-scoped ClaimIssue facts and never exposes them across tenants.
 *
 * @author wangguangwu
 */
public interface ClaimIssueRepository {
    void save(ClaimIssue claimIssue);

    Optional<ClaimIssue> findByTenantAndSignNoAndIssueNo(String tenantCode, String signNo, String issueNo);

    List<ClaimIssue> listByTenantAndSignNo(String tenantCode, String signNo);

    Optional<String> findHighestIssueNo(String tenantCode, String signNo);
}
