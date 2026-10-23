package com.claim.platform.intake.infrastructure.config;

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
@ConfigurationProperties(prefix = "claim.intake.submission.lock")
public class IntakeSubmissionLockProperties {

    @NotNull
    @DurationMin(millis = 0, message = "锁等待时间不能为负数")
    private Duration waitTime = Duration.ofSeconds(3);

    @NotNull
    @DurationMin(millis = 1, message = "锁租期必须大于 0")
    private Duration leaseTime = Duration.ofSeconds(10);

    @NotBlank
    private String keyPrefix = "claim:intake:submit:";
}
