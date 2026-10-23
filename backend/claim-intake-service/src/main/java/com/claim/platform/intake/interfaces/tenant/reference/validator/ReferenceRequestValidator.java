package com.claim.platform.intake.interfaces.tenant.reference.validator;

import com.claim.platform.intake.interfaces.tenant.core.exception.IntakeRequestValidationException;
import com.claim.platform.intake.interfaces.tenant.core.validator.RequestValidator;
import com.claim.platform.intake.interfaces.tenant.reference.ReferenceProtocolConstants;
import com.claim.platform.intake.interfaces.tenant.reference.dto.ReferenceIntakePayload;

/**
 * reference 参考协议的校验器：先执行平台公共校验，再追加协议差异规则。
 *
 * @author wangguangwu
 */
public class ReferenceRequestValidator implements RequestValidator<ReferenceIntakePayload> {

    private final RequestValidator<ReferenceIntakePayload> defaultValidator;

    public ReferenceRequestValidator(RequestValidator<ReferenceIntakePayload> defaultValidator) {
        this.defaultValidator = defaultValidator;
    }

    @Override
    public void validate(ReferenceIntakePayload request) {
        defaultValidator.validate(request);
        String relationship = request.applicant().relationshipToInsured();
        if (relationship == null || relationship.isBlank()) {
            throw new IntakeRequestValidationException(
                    ReferenceProtocolConstants.RELATIONSHIP_TO_INSURED_FIELD);
        }
    }
}
