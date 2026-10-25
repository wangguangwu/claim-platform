package com.claim.platform.sign.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.claim.platform.sign.domain.issue.model.ClaimIssue;
import com.claim.platform.sign.domain.issue.repository.ClaimIssueRepository;
import com.claim.platform.sign.infrastructure.persistence.dataobject.ClaimIssueDO;
import com.claim.platform.sign.infrastructure.persistence.mapper.ClaimIssueMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Enforces tenant and Sign scope on every problem-record lookup.
 *
 * @author wangguangwu
 */
@Repository
public class ClaimIssueRepositoryImpl implements ClaimIssueRepository {
    private final ClaimIssueMapper mapper;

    public ClaimIssueRepositoryImpl(ClaimIssueMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(ClaimIssue issue) {
        ClaimIssueDO data = new ClaimIssueDO();
        data.setTenantCode(issue.tenantCode());
        data.setSignNo(issue.signNo());
        data.setIssueNo(issue.issueNo());
        data.setAuditor(issue.auditor());
        data.setReason(issue.reason());
        data.setExtJson(issue.extensionJson());
        data.setIsSyncTenant(issue.syncedToTenant() ? 1 : 0);
        data.setIsSupplement(issue.requiresSupplement() ? 1 : 0);
        data.setIsDeleted(0);
        mapper.insert(data);
    }

    @Override
    public Optional<ClaimIssue> findByTenantAndSignNoAndIssueNo(String tenantCode, String signNo, String issueNo) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<ClaimIssueDO>lambdaQuery()
                .eq(ClaimIssueDO::getTenantCode, tenantCode).eq(ClaimIssueDO::getSignNo, signNo)
                .eq(ClaimIssueDO::getIssueNo, issueNo).eq(ClaimIssueDO::getIsDeleted, 0))).map(this::toDomain);
    }

    @Override
    public List<ClaimIssue> listByTenantAndSignNo(String tenantCode, String signNo) {
        return mapper.selectList(Wrappers.<ClaimIssueDO>lambdaQuery().eq(ClaimIssueDO::getTenantCode, tenantCode)
                        .eq(ClaimIssueDO::getSignNo, signNo).eq(ClaimIssueDO::getIsDeleted, 0))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<String> findHighestIssueNo(String tenantCode, String signNo) {
        return mapper.selectList(Wrappers.<ClaimIssueDO>lambdaQuery().eq(ClaimIssueDO::getTenantCode, tenantCode)
                        .eq(ClaimIssueDO::getSignNo, signNo).eq(ClaimIssueDO::getIsDeleted, 0)
                        .orderByDesc(ClaimIssueDO::getIssueNo).last("LIMIT 1"))
                .stream().findFirst().map(ClaimIssueDO::getIssueNo);
    }

    private ClaimIssue toDomain(ClaimIssueDO data) {
        return new ClaimIssue(data.getTenantCode(), data.getSignNo(), data.getIssueNo(), data.getAuditor(),
                data.getReason(), data.getExtJson(), data.getIsSyncTenant() == 1, data.getIsSupplement() == 1);
    }
}
