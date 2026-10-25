package com.claim.platform.sign.application.query;

import com.claim.platform.common.tenant.TenantContext;
import com.claim.platform.sign.application.exception.SignNotFoundException;
import com.claim.platform.sign.application.result.SignView;
import com.claim.platform.sign.domain.sign.model.Sign;
import com.claim.platform.sign.domain.sign.model.SignNo;
import com.claim.platform.sign.domain.sign.repository.SignRepository;
import org.springframework.stereotype.Service;

/** Reads Sign facts only through a trusted tenant context.
 *
 * @author wangguangwu
 */
@Service
public class SignQueryService {
    private final SignRepository repository;
    public SignQueryService(SignRepository repository) { this.repository = repository; }
    /** Returns the Sign projection or rejects access to an absent or foreign-tenant Sign. */
    public SignView getBySignNo(TenantContext context, String signNo) {
        Sign sign = repository.findByTenantAndSignNo(context.tenantCode(), new SignNo(signNo))
                .orElseThrow(() -> new SignNotFoundException(signNo));
        return new SignView(sign.signNo().value(), sign.externalApplyNo(), sign.applicantName(),
                sign.status().name(), sign.protocolVersion());
    }
}
