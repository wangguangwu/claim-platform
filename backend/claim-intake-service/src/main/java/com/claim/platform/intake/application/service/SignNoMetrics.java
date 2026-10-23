package com.claim.platform.intake.application.service;

import com.claim.platform.intake.application.result.SignNoRecoveryTrigger;
import com.claim.platform.intake.application.result.SignNoSource;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 记录签收号发号、降级、恢复、冲突和序列差值指标。
 * 指标标签只包含来源、结果、触发来源和冲突类型，不包含日期、租户、签收号或人员信息。
 *
 * @author wangguangwu
 */
@Component
public class SignNoMetrics {

    private static final String ALLOCATION_METRIC = "claim.intake.number.allocation";
    private static final String FALLBACK_METRIC = "claim.intake.number.fallback";
    private static final String RECOVERY_METRIC = "claim.intake.number.recovery";
    private static final String CONFLICT_METRIC = "claim.intake.number.conflict";
    private static final String SEQUENCE_GAP_METRIC = "claim.intake.number.sequence_gap";

    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicLong> sequenceGaps = new ConcurrentHashMap<>();

    public SignNoMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordAllocation(SignNoSource source, String result) {
        meterRegistry.counter(ALLOCATION_METRIC,
                "source", source.name().toLowerCase(Locale.ROOT), "result", result).increment();
    }

    public void recordFallback(String result) {
        meterRegistry.counter(FALLBACK_METRIC, "result", result).increment();
    }

    public void recordRecovery(SignNoRecoveryTrigger trigger, String result) {
        meterRegistry.counter(RECOVERY_METRIC, "trigger", trigger.code(), "result", result).increment();
    }

    public void recordConflict(String type) {
        meterRegistry.counter(CONFLICT_METRIC, "type", type).increment();
    }

    /**
     * 更新指定范围的 MySQL 高水位与 Redis 校准前计数器差值。
     *
     * @param scope {@code current} 或 {@code previous}
     */
    public void recordSequenceGap(String scope, long gap) {
        sequenceGaps.computeIfAbsent(scope, key -> {
            AtomicLong value = new AtomicLong(-1);
            Gauge.builder(SEQUENCE_GAP_METRIC, value, AtomicLong::doubleValue)
                    .tag("scope", key)
                    .register(meterRegistry);
            return value;
        }).set(gap);
    }
}
