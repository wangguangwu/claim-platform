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
 * 绑定签收幂等定位缓存的键前缀与存活时间，非法配置在启动时失败。
 *
 * @author wangguangwu
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "claim.intake.submission.replay-cache")
public class IntakeSubmissionReplayCacheProperties {

    @NotBlank
    private String keyPrefix = "claim:intake:replay:";

    @NotNull
    @DurationMin(millis = 1, message = "幂等定位缓存 TTL 必须大于 0")
    private Duration ttl = Duration.ofHours(24);
}
