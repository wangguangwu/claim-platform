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

    public static final String DEFAULT_ZONE = "Asia/Shanghai";
    public static final String DEFAULT_KEY_PREFIX = "sign:no:";
    public static final String KEY_PREFIX_TERMINATOR = ":";
    public static final int DEFAULT_COUNTER_TTL_DAYS = 3;
    public static final Duration DEFAULT_COUNTER_TTL = Duration.ofDays(DEFAULT_COUNTER_TTL_DAYS);
    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final long DEFAULT_RECOVERY_LOCK_WAIT_MILLIS = 1000L;
    public static final Duration DEFAULT_RECOVERY_LOCK_WAIT_TIME =
            Duration.ofMillis(DEFAULT_RECOVERY_LOCK_WAIT_MILLIS);
    public static final boolean DEFAULT_FALLBACK_ENABLED = true;
    public static final long DEFAULT_RECONCILIATION_INTERVAL_MINUTES = 5L;
    public static final Duration DEFAULT_RECONCILIATION_INTERVAL =
            Duration.ofMinutes(DEFAULT_RECONCILIATION_INTERVAL_MINUTES);
    public static final Duration DEFAULT_RECONCILIATION_INITIAL_DELAY = Duration.ZERO;
    public static final int DEFAULT_RECONCILIATION_DAYS = 2;

    @NotNull
    private ZoneId zone = ZoneId.of(DEFAULT_ZONE);

    @NotBlank
    private String keyPrefix = DEFAULT_KEY_PREFIX;

    @NotNull
    @DurationMin(days = 2, message = "签收号计数器 TTL 不能小于 2 天")
    private Duration counterTtl = DEFAULT_COUNTER_TTL;

    @Min(value = 1, message = "签收号最大尝试次数不能小于 1")
    @Max(value = 5, message = "签收号最大尝试次数不能大于 5")
    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

    @NotNull
    @DurationMin(millis = 0, message = "恢复锁等待时间不能为负数")
    private Duration recoveryLockWaitTime = DEFAULT_RECOVERY_LOCK_WAIT_TIME;

    private boolean fallbackEnabled = DEFAULT_FALLBACK_ENABLED;

    @NotNull
    @DurationMin(minutes = 1, message = "后台校准间隔不能小于 1 分钟")
    private Duration reconciliationInterval = DEFAULT_RECONCILIATION_INTERVAL;

    @NotNull
    @DurationMin(millis = 0, message = "后台校准初始延迟不能为负数")
    private Duration reconciliationInitialDelay = DEFAULT_RECONCILIATION_INITIAL_DELAY;

    @Min(value = 1, message = "后台校准日期数不能小于 1")
    @Max(value = 3, message = "后台校准日期数不能大于 3")
    private int reconciliationDays = DEFAULT_RECONCILIATION_DAYS;

    @AssertTrue(message = "签收号 Redis 键前缀必须以 ':' 结尾")
    public boolean isKeyPrefixTerminated() {
        return keyPrefix != null && keyPrefix.endsWith(KEY_PREFIX_TERMINATOR);
    }
}
