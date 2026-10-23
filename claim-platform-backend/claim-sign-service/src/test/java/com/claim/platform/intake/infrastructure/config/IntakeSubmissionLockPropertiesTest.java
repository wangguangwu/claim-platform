package com.claim.platform.intake.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

/**
 * 验证锁配置的安全默认值和非法值拒绝规则。
 *
 * @author wangguangwu
 */
class IntakeSubmissionLockPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldProvideSafeDefaults() {
        IntakeSubmissionLockProperties properties = new IntakeSubmissionLockProperties();

        assertThat(validator.validate(properties)).isEmpty();
        assertThat(properties.getWaitTime()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.getLeaseTime()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.getKeyPrefix()).isEqualTo("claim:intake:submit:");
    }

    @Test
    void shouldAcceptOverriddenValues() {
        IntakeSubmissionLockProperties properties = new IntakeSubmissionLockProperties();
        properties.setWaitTime(Duration.ofSeconds(1));
        properties.setLeaseTime(Duration.ofSeconds(5));
        properties.setKeyPrefix("custom:intake:");

        assertThat(validator.validate(properties)).isEmpty();
    }

    @Test
    void shouldRejectNegativeWaitTime() {
        IntakeSubmissionLockProperties properties = new IntakeSubmissionLockProperties();
        properties.setWaitTime(Duration.ofSeconds(-1));

        assertViolation(properties, "锁等待时间不能为负数");
    }

    @Test
    void shouldRejectZeroLeaseTime() {
        IntakeSubmissionLockProperties properties = new IntakeSubmissionLockProperties();
        properties.setLeaseTime(Duration.ZERO);

        assertViolation(properties, "锁租期必须大于 0");
    }

    @Test
    void shouldRejectBlankKeyPrefix() {
        IntakeSubmissionLockProperties properties = new IntakeSubmissionLockProperties();
        properties.setKeyPrefix("  ");

        assertViolation(properties, "不能为空");
    }

    private void assertViolation(IntakeSubmissionLockProperties properties, String messageFragment) {
        Set<ConstraintViolation<IntakeSubmissionLockProperties>> violations = validator.validate(properties);
        assertThat(violations).anyMatch(violation -> violation.getMessage().contains(messageFragment));
    }
}
