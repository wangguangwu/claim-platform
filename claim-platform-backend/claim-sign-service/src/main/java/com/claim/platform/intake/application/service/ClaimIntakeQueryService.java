package com.claim.platform.intake.application.service;

import com.claim.platform.intake.application.result.ClaimIntakeDetailResult;
import com.claim.platform.intake.domain.exception.ClaimIntakeNotFoundException;
import com.claim.platform.intake.domain.model.ClaimIntakeDetails;
import com.claim.platform.intake.domain.repository.ClaimIntakeStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在当前租户边界内提供签收申请查询，不越过 Claim Intake 数据所有权。
 *
 * @author wangguangwu
 */
@Service
public class ClaimIntakeQueryService {

    private final ClaimIntakeStore intakeStore;

    public ClaimIntakeQueryService(ClaimIntakeStore intakeStore) {
        this.intakeStore = intakeStore;
    }

    /**
     * 查询本租户签收申请与未删除材料；跨租户编号按不存在处理。
     *
     * @return 可安全返回给调用方的申请详情
     */
    @Transactional(readOnly = true)
    public ClaimIntakeDetailResult get(String tenantCode, String signNo) {
        ClaimIntakeDetails details = intakeStore.findDetailsByTenantAndSignNo(tenantCode, signNo)
                .orElseThrow(ClaimIntakeNotFoundException::new);
        return new ClaimIntakeDetailResult(details.intake().signNo(), details.intake().externalApplyNo(),
                details.intake().applicantName(), details.intake().status(), details.materials());
    }
}
