package com.claim.platform.intake.infrastructure.config;

import com.claim.platform.intake.application.port.SignNoPolicy;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.ZoneId;

/**
 * 绑定签收号生成的时区、Redis 键与 TTL、重试次数和后台校准参数，非法配置在启动时失败。
 *
 * @author wangguangwu
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "claim.intake.number")
public class SignNoProperties implements SignNoPolicy {

    @NotNull
    private ZoneId zone = ZoneId.of("Asia/Shanghai");

    @NotBlank
    private String keyPrefix = "sign:no:";

    @NotNull
    @DurationMin(days = 2, message = "签收号计数器 TTL 不能小于 2 天")
    private Duration counterTtl = Duration.ofDays(3);

    @Min(value = 1, message = "签收号最大尝试次数不能小于 1")
    @Max(value = 5, message = "签收号最大尝试次数不能大于 5")
    private int maxAttempts = 3;

    @NotNull
    @DurationMin(millis = 0, message = "恢复锁等待时间不能为负数")
    private Duration recoveryLockWaitTime = Duration.ofSeconds(1);

    private boolean fallbackEnabled = true;

    @NotNull
    @DurationMin(minutes = 1, message = "后台校准间隔不能小于 1 分钟")
    private Duration reconciliationInterval = Duration.ofMinutes(5);

    @NotNull
    @DurationMin(millis = 0, message = "后台校准初始延迟不能为负数")
    private Duration reconciliationInitialDelay = Duration.ZERO;

    @Min(value = 1, message = "后台校准日期数不能小于 1")
    @Max(value = 3, message = "后台校准日期数不能大于 3")
    private int reconciliationDays = 2;

    @AssertTrue(message = "签收号 Redis 键前缀必须以 ':' 结尾")
    public boolean isKeyPrefixTerminated() {
        return keyPrefix != null && keyPrefix.endsWith(":");
    }
}
