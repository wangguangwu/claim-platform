package com.claim.platform.sign.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.redisson.config.Config;

import java.time.Duration;

/**
 * 验证签收业务锁的基础租期映射为 Redisson watchdog 超时。
 *
 * @author wangguangwu
 */
class IntakeSubmissionLockConfigurationTest {

    private final IntakeSubmissionLockConfiguration configuration = new IntakeSubmissionLockConfiguration();

    @Test
    void shouldMapLeaseTimeToLockWatchdogTimeout() {
        IntakeSubmissionLockProperties properties = new IntakeSubmissionLockProperties();
        properties.setLeaseTime(Duration.ofSeconds(7));
        Config config = new Config();

        configuration.intakeSubmissionLockWatchdogCustomizer(properties).customize(config);

        assertThat(config.getLockWatchdogTimeout()).isEqualTo(7000L);
    }

    @Test
    void shouldMapDefaultLeaseTimeToLockWatchdogTimeout() {
        IntakeSubmissionLockProperties properties = new IntakeSubmissionLockProperties();
        Config config = new Config();

        configuration.intakeSubmissionLockWatchdogCustomizer(properties).customize(config);

        assertThat(config.getLockWatchdogTimeout()).isEqualTo(Duration.ofSeconds(10).toMillis());
    }
}
