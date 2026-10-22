package com.claim.platform.application.usecase;

import com.claim.platform.application.usecase.result.SignApplicationDetailResult;
import com.claim.platform.application.domain.exception.SignApplicationNotFoundException;
import com.claim.platform.application.domain.model.SignApplicationDetails;
import com.claim.platform.application.domain.repository.SignApplicationStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在当前租户边界内提供签收申请查询，不越过 Application Intake 数据所有权。
 *
 * @author wangguangwu
 */
@Service
public class SignApplicationQueryService {

    private final SignApplicationStore applicationStore;

    public SignApplicationQueryService(SignApplicationStore applicationStore) {
        this.applicationStore = applicationStore;
    }

    /**
     * 查询本租户签收申请与未删除材料；跨租户编号按不存在处理。
     *
     * @return 可安全返回给调用方的申请详情
     */
    @Transactional(readOnly = true)
    public SignApplicationDetailResult get(String tenantCode, String signNo) {
        SignApplicationDetails details = applicationStore.findDetailsByTenantAndSignNo(tenantCode, signNo)
                .orElseThrow(SignApplicationNotFoundException::new);
        return new SignApplicationDetailResult(details.application().signNo(), details.application().externalApplyNo(),
                details.application().applicantName(), details.application().status(), details.materials());
    }
}
