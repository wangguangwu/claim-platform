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
class SignSubmissionReplayCachePropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldProvideSafeDefaults() {
        SignSubmissionReplayCacheProperties properties = new SignSubmissionReplayCacheProperties();

        assertThat(validator.validate(properties)).isEmpty();
        assertThat(properties.getKeyPrefix()).isEqualTo("claim:intake:replay:");
        assertThat(properties.getTtl()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void shouldAcceptOverriddenValues() {
        SignSubmissionReplayCacheProperties properties = new SignSubmissionReplayCacheProperties();
        properties.setKeyPrefix("custom:sign:replay:");
        properties.setTtl(Duration.ofHours(1));

        assertThat(validator.validate(properties)).isEmpty();
    }

    @Test
    void shouldRejectBlankKeyPrefix() {
        SignSubmissionReplayCacheProperties properties = new SignSubmissionReplayCacheProperties();
        properties.setKeyPrefix("  ");

        assertViolation(properties, "不能为空");
    }

    @Test
    void shouldRejectZeroTtl() {
        SignSubmissionReplayCacheProperties properties = new SignSubmissionReplayCacheProperties();
        properties.setTtl(Duration.ZERO);

        assertViolation(properties, "幂等定位缓存 TTL 必须大于 0");
    }

    private void assertViolation(SignSubmissionReplayCacheProperties properties, String messageFragment) {
        Set<ConstraintViolation<SignSubmissionReplayCacheProperties>> violations = validator.validate(properties);
        assertThat(violations).anyMatch(violation -> violation.getMessage().contains(messageFragment));
    }
}
