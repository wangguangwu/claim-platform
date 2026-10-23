package com.claim.platform.sign.interfaces.tenant.support;

import com.claim.platform.sign.interfaces.tenant.core.exception.IntakeRequestValidationException;
import com.claim.platform.sign.interfaces.tenant.core.validator.RequestValidator;

/**
 * 测试租户校验器：先执行平台公共校验，再追加租户差异规则。
 *
 * @author wangguangwu
 */
public class SupportTenantRequestValidator implements RequestValidator<SupportTenantIntakePayload> {

    private final RequestValidator<SupportTenantIntakePayload> defaultValidator;

    public SupportTenantRequestValidator(RequestValidator<SupportTenantIntakePayload> defaultValidator) {
        this.defaultValidator = defaultValidator;
    }

    @Override
    public void validate(SupportTenantIntakePayload request) {
        defaultValidator.validate(request);
        String relationship = request.applicant().relationshipToInsured();
        if (relationship == null || relationship.isBlank()) {
            throw new IntakeRequestValidationException("applicant.relationshipToInsured");
        }
    }
}
