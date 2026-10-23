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

    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicLong> sequenceGaps = new ConcurrentHashMap<>();

    public SignNoMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordAllocation(SignNoSource source, String result) {
        meterRegistry.counter(IntakeMetricConstants.SIGN_NO_ALLOCATION_METRIC,
                IntakeMetricConstants.TAG_SOURCE, source.name().toLowerCase(Locale.ROOT),
                IntakeMetricConstants.TAG_RESULT, result).increment();
    }

    public void recordFallback(String result) {
        meterRegistry.counter(IntakeMetricConstants.SIGN_NO_FALLBACK_METRIC,
                IntakeMetricConstants.TAG_RESULT, result).increment();
    }

    public void recordRecovery(SignNoRecoveryTrigger trigger, String result) {
        meterRegistry.counter(IntakeMetricConstants.SIGN_NO_RECOVERY_METRIC,
                IntakeMetricConstants.TAG_TRIGGER, trigger.code(),
                IntakeMetricConstants.TAG_RESULT, result).increment();
    }

    public void recordConflict(String type) {
        meterRegistry.counter(IntakeMetricConstants.SIGN_NO_CONFLICT_METRIC,
                IntakeMetricConstants.TAG_TYPE, type).increment();
    }

    /**
     * 更新指定范围的 MySQL 高水位与 Redis 校准前计数器差值。
     *
     * @param scope {@code current} 或 {@code previous}
     */
    public void recordSequenceGap(String scope, long gap) {
        sequenceGaps.computeIfAbsent(scope, key -> {
            AtomicLong value = new AtomicLong(-1);
            Gauge.builder(IntakeMetricConstants.SIGN_NO_SEQUENCE_GAP_METRIC, value, AtomicLong::doubleValue)
                    .tag(IntakeMetricConstants.TAG_SCOPE, key)
                    .register(meterRegistry);
            return value;
        }).set(gap);
    }
}
