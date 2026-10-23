package com.claim.platform.sign.interfaces.tenant.support;

import com.claim.platform.sign.interfaces.tenant.core.exception.SignRequestValidationException;
import com.claim.platform.sign.interfaces.tenant.core.validator.RequestValidator;

/**
 * 测试租户校验器：先执行平台公共校验，再追加租户差异规则。
 *
 * @author wangguangwu
 */
public class SupportTenantRequestValidator implements RequestValidator<SupportTenantSignPayload> {

    private final RequestValidator<SupportTenantSignPayload> defaultValidator;

    public SupportTenantRequestValidator(RequestValidator<SupportTenantSignPayload> defaultValidator) {
        this.defaultValidator = defaultValidator;
    }

    @Override
    public void validate(SupportTenantSignPayload request) {
        defaultValidator.validate(request);
        String relationship = request.applicant().relationshipToInsured();
        if (relationship == null || relationship.isBlank()) {
            throw new SignRequestValidationException("applicant.relationshipToInsured");
        }
    }
}
