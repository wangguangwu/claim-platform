package com.claim.platform.sign.application.service;

import com.claim.platform.sign.application.result.SignDetailResult;
import com.claim.platform.sign.domain.exception.SignNotFoundException;
import com.claim.platform.sign.domain.model.SignDetails;
import com.claim.platform.sign.domain.repository.SignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在当前租户边界内提供签收申请查询，不越过理赔签收数据所有权。
 *
 * @author wangguangwu
 */
@Service
public class SignQueryService {

    private final SignRepository signRepository;

    public SignQueryService(SignRepository signRepository) {
        this.signRepository = signRepository;
    }

    /**
     * 查询本租户签收申请与未删除材料；跨租户编号按不存在处理。
     *
     * @return 可安全返回给调用方的申请详情
     */
    @Transactional(readOnly = true)
    public SignDetailResult get(String tenantCode, String signNo) {
        SignDetails details = signRepository.findDetailsByTenantAndSignNo(tenantCode, signNo)
                .orElseThrow(SignNotFoundException::new);
        return new SignDetailResult(details.sign().signNo(), details.sign().externalApplyNo(),
                details.sign().applicantName(), details.sign().status(), details.materials());
    }
}
