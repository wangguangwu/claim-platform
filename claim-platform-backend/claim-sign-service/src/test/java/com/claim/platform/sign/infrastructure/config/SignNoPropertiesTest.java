package com.claim.platform.sign.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Set;

/**
 * 验证签收号配置的安全默认值和非法值拒绝规则。
 *
 * @author wangguangwu
 */
class SignNoPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldProvideSafeDefaults() {
        SignNoProperties properties = new SignNoProperties();

        assertThat(validator.validate(properties)).isEmpty();
        assertThat(properties.getZone()).isEqualTo(ZoneId.of("Asia/Shanghai"));
        assertThat(properties.getKeyPrefix()).isEqualTo("sign:no:");
        assertThat(properties.getCounterTtl()).isEqualTo(Duration.ofDays(3));
        assertThat(properties.getMaxAttempts()).isEqualTo(3);
        assertThat(properties.getRecoveryLockWaitTime()).isEqualTo(Duration.ofSeconds(1));
        assertThat(properties.isFallbackEnabled()).isTrue();
        assertThat(properties.getReconciliationInterval()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.getReconciliationInitialDelay()).isZero();
        assertThat(properties.getReconciliationDays()).isEqualTo(2);
    }

    @Test
    void shouldAcceptOverriddenValues() {
        SignNoProperties properties = new SignNoProperties();
        properties.setZone(ZoneId.of("UTC"));
        properties.setKeyPrefix("custom:no:");
        properties.setCounterTtl(Duration.ofDays(2));
        properties.setMaxAttempts(5);
        properties.setRecoveryLockWaitTime(Duration.ZERO);
        properties.setFallbackEnabled(false);
        properties.setReconciliationInterval(Duration.ofMinutes(1));
        properties.setReconciliationInitialDelay(Duration.ofSeconds(10));
        properties.setReconciliationDays(3);

        assertThat(validator.validate(properties)).isEmpty();
    }

    @Test
    void shouldRejectBlankKeyPrefix() {
        SignNoProperties properties = new SignNoProperties();
        properties.setKeyPrefix("  ");

        assertViolation(properties, "不能为空");
    }

    @Test
    void shouldRejectKeyPrefixWithoutColon() {
        SignNoProperties properties = new SignNoProperties();
        properties.setKeyPrefix("sign-no");

        assertViolation(properties, "必须以 ':' 结尾");
    }

    @Test
    void shouldRejectTooShortCounterTtl() {
        SignNoProperties properties = new SignNoProperties();
        properties.setCounterTtl(Duration.ofHours(47));

        assertViolation(properties, "TTL 不能小于 2 天");
    }

    @Test
    void shouldRejectOutOfRangeMaxAttempts() {
        SignNoProperties low = new SignNoProperties();
        low.setMaxAttempts(0);
        SignNoProperties high = new SignNoProperties();
        high.setMaxAttempts(6);

        assertViolation(low, "最大尝试次数不能小于 1");
        assertViolation(high, "最大尝试次数不能大于 5");
    }

    @Test
    void shouldRejectNegativeLockWaitTime() {
        SignNoProperties properties = new SignNoProperties();
        properties.setRecoveryLockWaitTime(Duration.ofMillis(-1));

        assertViolation(properties, "恢复锁等待时间不能为负数");
    }

    @Test
    void shouldRejectTooShortReconciliationInterval() {
        SignNoProperties properties = new SignNoProperties();
        properties.setReconciliationInterval(Duration.ofSeconds(59));

        assertViolation(properties, "后台校准间隔不能小于 1 分钟");
    }

    @Test
    void shouldRejectNegativeInitialDelay() {
        SignNoProperties properties = new SignNoProperties();
        properties.setReconciliationInitialDelay(Duration.ofMillis(-1));

        assertViolation(properties, "初始延迟不能为负数");
    }

    @Test
    void shouldRejectOutOfRangeReconciliationDays() {
        SignNoProperties low = new SignNoProperties();
        low.setReconciliationDays(0);
        SignNoProperties high = new SignNoProperties();
        high.setReconciliationDays(4);

        assertViolation(low, "校准日期数不能小于 1");
        assertViolation(high, "校准日期数不能大于 3");
    }

    private void assertViolation(SignNoProperties properties, String messageFragment) {
        Set<ConstraintViolation<SignNoProperties>> violations = validator.validate(properties);
        assertThat(violations).anyMatch(violation -> violation.getMessage().contains(messageFragment));
    }
}
