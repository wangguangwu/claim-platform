package com.claim.platform.sign.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 绑定签收提交业务锁的等待时间、watchdog 基础租期和键前缀，非法配置在启动时失败。
 *
 * @author wangguangwu
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "claim.sign.submission.lock")
public class SignSubmissionLockProperties {

    public static final long DEFAULT_WAIT_TIME_MILLIS = 3000L;
    public static final Duration DEFAULT_WAIT_TIME = Duration.ofMillis(DEFAULT_WAIT_TIME_MILLIS);
    public static final long DEFAULT_LEASE_TIME_SECONDS = 10L;
    public static final Duration DEFAULT_LEASE_TIME = Duration.ofSeconds(DEFAULT_LEASE_TIME_SECONDS);
    public static final String DEFAULT_KEY_PREFIX = "claim:intake:submit:";

    @NotNull
    @DurationMin(millis = 0, message = "锁等待时间不能为负数")
    private Duration waitTime = DEFAULT_WAIT_TIME;

    @NotNull
    @DurationMin(millis = 1, message = "锁租期必须大于 0")
    private Duration leaseTime = DEFAULT_LEASE_TIME;

    @NotBlank
    private String keyPrefix = DEFAULT_KEY_PREFIX;
}
