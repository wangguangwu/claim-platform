package com.claim.platform.sign.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 验证实例级 Redis 就绪信号的初始值和切换语义。
 *
 * @author wangguangwu
 */
class SignNoModeCoordinatorTest {

    @Test
    void shouldStartUnreadyAndSwitchBetweenStates() {
        SignNoModeCoordinator coordinator = new SignNoModeCoordinator();

        assertThat(coordinator.isRedisReady()).isFalse();

        coordinator.markRedisReady();
        assertThat(coordinator.isRedisReady()).isTrue();

        coordinator.markRedisUnavailable();
        assertThat(coordinator.isRedisReady()).isFalse();

        coordinator.markRedisReady();
        assertThat(coordinator.isRedisReady()).isTrue();
    }
}
