package com.claim.platform.sign.interfaces.tenant.reference.validator;

import com.claim.platform.sign.interfaces.tenant.core.exception.SignRequestValidationException;
import com.claim.platform.sign.interfaces.tenant.core.validator.RequestValidator;
import com.claim.platform.sign.interfaces.tenant.reference.ReferenceProtocolConstants;
import com.claim.platform.sign.interfaces.tenant.reference.dto.ReferenceSignPayload;

/**
 * reference 参考协议的校验器：先执行平台公共校验，再追加协议差异规则。
 *
 * @author wangguangwu
 */
public class ReferenceRequestValidator implements RequestValidator<ReferenceSignPayload> {

    private final RequestValidator<ReferenceSignPayload> defaultValidator;

    public ReferenceRequestValidator(RequestValidator<ReferenceSignPayload> defaultValidator) {
        this.defaultValidator = defaultValidator;
    }

    @Override
    public void validate(ReferenceSignPayload request) {
        defaultValidator.validate(request);
        String relationship = request.applicant().relationshipToInsured();
        if (relationship == null || relationship.isBlank()) {
            throw new SignRequestValidationException(
                    ReferenceProtocolConstants.RELATIONSHIP_TO_INSURED_FIELD);
        }
    }
}
