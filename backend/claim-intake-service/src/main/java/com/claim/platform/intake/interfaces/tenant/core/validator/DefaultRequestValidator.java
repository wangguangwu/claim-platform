package com.claim.platform.intake.interfaces.tenant.core.validator;

import com.claim.platform.intake.interfaces.tenant.core.exception.IntakeRequestValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Objects;
import java.util.Set;

/**
 * 基于 Jakarta Bean Validation 的默认校验器，执行租户 DTO 上声明的公共规则。
 * 租户校验器先委托本类完成公共校验，再追加自身差异规则。
 *
 * @param <T> 当前租户的报文 DTO 类型
 * @author wangguangwu
 */
public class DefaultRequestValidator<T> implements RequestValidator<T> {

    private final Validator validator;

    public DefaultRequestValidator(Validator validator) {
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    @Override
    public void validate(T request) {
        if (request == null) {
            throw new IntakeRequestValidationException("request");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (violations.isEmpty()) {
            return;
        }
        String fieldPath = violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .filter(path -> !path.isBlank())
                .sorted()
                .findFirst()
                .orElse("request");
        throw new IntakeRequestValidationException(fieldPath);
    }
}
