package com.claim.platform.sign.application.query;

import com.claim.platform.common.tenant.TenantContext;
import com.claim.platform.sign.application.result.ClaimIssueView;
import com.claim.platform.sign.domain.issue.repository.ClaimIssueRepository;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Reads platform ClaimIssue facts exclusively within a trusted tenant and Sign scope.
 *
 * @author wangguangwu
 */
@Service
public class ClaimIssueQueryService {
    private final ClaimIssueRepository repository;

    public ClaimIssueQueryService(ClaimIssueRepository repository) {
        this.repository = repository;
    }

    /**
     * Lists issue projections bound to one tenant Sign.
     */
    public List<ClaimIssueView> listBySignNo(TenantContext context, String signNo) {
        return repository.listByTenantAndSignNo(context.tenantCode(), signNo).stream()
                .map(value -> new ClaimIssueView(value.issueNo(), value.auditor(), value.reason(),
                        value.extensionJson(), value.syncedToTenant(), value.requiresSupplement())).toList();
    }
}
