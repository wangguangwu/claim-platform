package com.claim.platform.intake.application.service;

import com.claim.platform.intake.application.exception.SignNoSequenceExhaustedException;
import com.claim.platform.intake.application.exception.SignNoUnavailableException;
import com.claim.platform.intake.application.exception.SignNoUnavailableReason;
import com.claim.platform.intake.application.port.SignNoFallbackAllocator;
import com.claim.platform.intake.application.port.SignNoIncrementResult;
import com.claim.platform.intake.application.port.SignNoPolicy;
import com.claim.platform.intake.application.port.SignNoSequenceCounter;
import com.claim.platform.intake.application.result.ReconciliationStatus;
import com.claim.platform.intake.application.result.SignNoAllocation;
import com.claim.platform.intake.application.result.SignNoReconcileAllocationOutcome;
import com.claim.platform.intake.application.result.SignNoReconciliationResult;
import com.claim.platform.intake.application.result.SignNoRecoveryTrigger;
import com.claim.platform.intake.application.result.SignNoSource;
import com.claim.platform.intake.domain.model.SignNoFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 按实例 Redis 就绪状态选择发号路径，并在计数器异常、Redis 故障和唯一冲突后完成恢复。
 * 正常路径只执行一次 Redis 原子自增；Redis 不可用时按配置降级到 MySQL 高水位。
 *
 * @author wangguangwu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignNoAllocationService {

    private final SignNoSequenceCounter counter;
    private final SignNoReconciliationService reconciliationService;
    private final SignNoFallbackAllocator fallbackAllocator;
    private final SignNoModeCoordinator modeCoordinator;
    private final SignNoPolicy signNoPolicy;
    private final SignNoMetrics metrics;
    private final Clock clock;

    /**
     * 分配下一个签收号；每次调用只计算一次业务日期。
     *
     * @throws SignNoUnavailableException       两条路径都不可用时抛出
     * @throws SignNoSequenceExhaustedException 当日流水号耗尽时抛出
     */
    public SignNoAllocation next() {
        LocalDate businessDate = LocalDate.now(clock);
        if (!modeCoordinator.isRedisReady()) {
            return allocateFromFallback(businessDate, null);
        }
        return allocateFromRedis(businessDate);
    }

    /**
     * 唯一键冲突后按失败编号的业务日期校准 Redis；降级来源和未就绪实例不访问 Redis。
     */
    public void recoverAfterConflict(LocalDate businessDate, SignNoSource source) {
        if (source == SignNoSource.MYSQL_FALLBACK || !modeCoordinator.isRedisReady()) {
            return;
        }
        SignNoReconciliationResult result =
                reconciliationService.reconcile(businessDate, SignNoRecoveryTrigger.UNIQUE_CONFLICT);
        if (result.status() == ReconciliationStatus.SUCCESS) {
            metrics.recordRecovery(SignNoRecoveryTrigger.UNIQUE_CONFLICT, "success");
        } else if (result.status() == ReconciliationStatus.SKIPPED) {
            metrics.recordRecovery(SignNoRecoveryTrigger.UNIQUE_CONFLICT, "skipped");
        } else {
            metrics.recordRecovery(SignNoRecoveryTrigger.UNIQUE_CONFLICT, "failure");
        }
        if (result.failureReason() != null && result.failureReason().isRedisSide()) {
            modeCoordinator.markRedisUnavailable();
        }
    }

    private SignNoAllocation allocateFromRedis(LocalDate businessDate) {
        SignNoRecoveryTrigger trigger = null;
        try {
            SignNoIncrementResult result = counter.tryIncrement(businessDate);
            if (result instanceof SignNoIncrementResult.Allocated allocated) {
                SignNoAllocation allocation = toAllocation(businessDate, allocated.sequence(), SignNoSource.REDIS);
                metrics.recordAllocation(SignNoSource.REDIS, "allocated");
                return allocation;
            }
            trigger = result instanceof SignNoIncrementResult.Missing
                    ? SignNoRecoveryTrigger.MISSING_COUNTER
                    : SignNoRecoveryTrigger.INVALID_COUNTER;
            SignNoReconcileAllocationOutcome outcome =
                    reconciliationService.reconcileAndAllocate(businessDate, trigger);
            SignNoAllocation allocation = toAllocation(businessDate, outcome.sequence(), SignNoSource.REDIS);
            metrics.recordRecovery(trigger, "success");
            metrics.recordAllocation(SignNoSource.REDIS, "recovered");
            return allocation;
        } catch (SignNoSequenceExhaustedException exception) {
            metrics.recordAllocation(SignNoSource.REDIS, "exhausted");
            log.error("claim intake sign no exhausted date={} source={}", businessDate, SignNoSource.REDIS);
            throw exception;
        } catch (SignNoUnavailableException exception) {
            if (trigger != null && exception.reason() == SignNoUnavailableReason.RECOVERY_LOCK_TIMEOUT) {
                SignNoAllocation retried = retryAfterRecoveryLockTimeout(businessDate, trigger);
                if (retried != null) {
                    return retried;
                }
            }
            if (trigger != null) {
                metrics.recordRecovery(trigger, "failure");
            }
            metrics.recordAllocation(SignNoSource.REDIS, "unavailable");
            log.warn("claim intake sign no redis allocation failed date={} reason={}", businessDate,
                    exception.reason());
            if (exception.reason().isRedisSide()) {
                modeCoordinator.markRedisUnavailable();
                return allocateFromFallback(businessDate, exception);
            }
            throw exception;
        }
    }

    private SignNoAllocation retryAfterRecoveryLockTimeout(LocalDate businessDate, SignNoRecoveryTrigger trigger) {
        try {
            SignNoIncrementResult retry = counter.tryIncrement(businessDate);
            if (retry instanceof SignNoIncrementResult.Allocated allocated) {
                SignNoAllocation allocation = toAllocation(businessDate, allocated.sequence(), SignNoSource.REDIS);
                metrics.recordRecovery(trigger, "success");
                metrics.recordAllocation(SignNoSource.REDIS, "recovered");
                return allocation;
            }
            return null;
        } catch (SignNoUnavailableException | SignNoSequenceExhaustedException exception) {
            return null;
        }
    }

    private SignNoAllocation allocateFromFallback(LocalDate businessDate, SignNoUnavailableException cause) {
        if (!signNoPolicy.isFallbackEnabled()) {
            if (cause != null) {
                throw cause;
            }
            throw new SignNoUnavailableException(SignNoUnavailableReason.FALLBACK_DISABLED);
        }
        try {
            long sequence = fallbackAllocator.nextSequence(businessDate);
            SignNoAllocation allocation = toAllocation(businessDate, sequence, SignNoSource.MYSQL_FALLBACK);
            metrics.recordFallback("success");
            metrics.recordAllocation(SignNoSource.MYSQL_FALLBACK, "allocated");
            log.info("claim intake sign no fallback allocated date={} sequence={}", businessDate, sequence);
            return allocation;
        } catch (SignNoSequenceExhaustedException exception) {
            metrics.recordFallback("exhausted");
            metrics.recordAllocation(SignNoSource.MYSQL_FALLBACK, "exhausted");
            log.error("claim intake sign no exhausted date={} source={}", businessDate,
                    SignNoSource.MYSQL_FALLBACK);
            throw exception;
        } catch (SignNoUnavailableException exception) {
            metrics.recordFallback("unavailable");
            metrics.recordAllocation(SignNoSource.MYSQL_FALLBACK, "unavailable");
            throw exception;
        }
    }

    private SignNoAllocation toAllocation(LocalDate businessDate, long sequence, SignNoSource source) {
        if (sequence > SignNoFormat.MAX_SEQUENCE) {
            throw new SignNoSequenceExhaustedException();
        }
        if (sequence < 1) {
            SignNoUnavailableReason reason = source == SignNoSource.REDIS
                    ? SignNoUnavailableReason.REDIS_UNAVAILABLE
                    : SignNoUnavailableReason.FALLBACK_UNAVAILABLE;
            throw new SignNoUnavailableException(reason);
        }
        return new SignNoAllocation(SignNoFormat.format(businessDate, sequence), source);
    }
}
