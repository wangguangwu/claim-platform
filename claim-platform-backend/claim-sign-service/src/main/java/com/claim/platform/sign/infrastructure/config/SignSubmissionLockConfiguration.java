package com.claim.platform.sign.infrastructure.config;

import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 将签收业务锁的基础租期配置为 Redisson watchdog 超时。
 * 锁持有期间 Redisson 按超时的三分之一自动续期；进程退出后锁最迟在一个基础租期后释放。
 *
 * @author wangguangwu
 */
@Configuration
public class SignSubmissionLockConfiguration {

    @Bean
    public RedissonAutoConfigurationCustomizer signSubmissionLockWatchdogCustomizer(
            SignSubmissionLockProperties properties) {
        return config -> config.setLockWatchdogTimeout(properties.getLeaseTime().toMillis());
    }
}
