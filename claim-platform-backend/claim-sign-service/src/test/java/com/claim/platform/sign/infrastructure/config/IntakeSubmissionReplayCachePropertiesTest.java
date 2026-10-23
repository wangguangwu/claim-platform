package com.claim.platform.sign.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

/**
 * 验证签收幂等定位缓存配置的安全默认值和非法值拒绝规则。
 *
 * @author wangguangwu
 */
class IntakeSubmissionReplayCachePropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldProvideSafeDefaults() {
        IntakeSubmissionReplayCacheProperties properties = new IntakeSubmissionReplayCacheProperties();

        assertThat(validator.validate(properties)).isEmpty();
        assertThat(properties.getKeyPrefix()).isEqualTo("claim:intake:replay:");
        assertThat(properties.getTtl()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void shouldAcceptOverriddenValues() {
        IntakeSubmissionReplayCacheProperties properties = new IntakeSubmissionReplayCacheProperties();
        properties.setKeyPrefix("custom:intake:replay:");
        properties.setTtl(Duration.ofHours(1));

        assertThat(validator.validate(properties)).isEmpty();
    }

    @Test
    void shouldRejectBlankKeyPrefix() {
        IntakeSubmissionReplayCacheProperties properties = new IntakeSubmissionReplayCacheProperties();
        properties.setKeyPrefix("  ");

        assertViolation(properties, "不能为空");
    }

    @Test
    void shouldRejectZeroTtl() {
        IntakeSubmissionReplayCacheProperties properties = new IntakeSubmissionReplayCacheProperties();
        properties.setTtl(Duration.ZERO);

        assertViolation(properties, "幂等定位缓存 TTL 必须大于 0");
    }

    private void assertViolation(IntakeSubmissionReplayCacheProperties properties, String messageFragment) {
        Set<ConstraintViolation<IntakeSubmissionReplayCacheProperties>> violations = validator.validate(properties);
        assertThat(violations).anyMatch(violation -> violation.getMessage().contains(messageFragment));
    }
}
