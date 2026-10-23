package com.claim.platform.sign.infrastructure.scheduling;

import com.claim.platform.sign.application.result.ReconciliationStatus;
import com.claim.platform.sign.application.result.SignNoReconciliationResult;
import com.claim.platform.sign.application.result.SignNoRecoveryTrigger;
import com.claim.platform.sign.application.service.SignMetricConstants;
import com.claim.platform.sign.application.service.SignNoMetrics;
import com.claim.platform.sign.application.service.SignNoModeCoordinator;
import com.claim.platform.sign.application.service.SignNoReconciliationService;
import com.claim.platform.sign.infrastructure.config.SignNoProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 在单线程后台执行器中完成启动、周期和人工触发的签收号校准。
 * 每次处理当日及配置数量的前一业务日期，重叠运行直接跳过；
 * 单日失败只记录日志和指标，不阻断其他日期，也不影响请求处理。
 *
 * @author wangguangwu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignNoReconciliationScheduler {

    private static final String THREAD_NAME = "sign-no-reconciler";
    private static final long SHUTDOWN_WAIT_SECONDS = 3;

    private final SignNoReconciliationService reconciliationService;
    private final SignNoModeCoordinator modeCoordinator;
    private final SignNoMetrics metrics;
    private final SignNoProperties properties;
    private final Clock clock;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean startupRound = new AtomicBoolean(true);

    private ScheduledExecutorService executor;

    @PostConstruct
    public void start() {
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::scheduledRound,
                properties.getReconciliationInitialDelay().toMillis(),
                properties.getReconciliationInterval().toMillis(),
                TimeUnit.MILLISECONDS);
        log.info("claim sign no reconciliation scheduler started initialDelay={} interval={} days={}",
                properties.getReconciliationInitialDelay(), properties.getReconciliationInterval(),
                properties.getReconciliationDays());
    }

    @PreDestroy
    public void stop() {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 提交一次异步人工校准；已有校准轮次运行时立即返回跳过结果，不阻塞调用线程。
     *
     * @param businessDate 待校准业务日期
     * @return 异步校准结果
     */
    public CompletableFuture<SignNoReconciliationResult> requestAsync(LocalDate businessDate) {
        if (running.get()) {
            metrics.recordRecovery(SignNoRecoveryTrigger.MANUAL, SignMetricConstants.RESULT_SKIPPED);
            log.info("claim sign no manual reconciliation skipped date={} reason=overlap", businessDate);
            return CompletableFuture.completedFuture(SignNoReconciliationResult.skipped(businessDate));
        }
        ScheduledExecutorService currentExecutor = executor;
        if (currentExecutor == null) {
            throw new IllegalStateException("签收号校准调度器尚未启动");
        }
        return CompletableFuture.supplyAsync(() -> manualRound(businessDate), currentExecutor);
    }

    private void scheduledRound() {
        SignNoRecoveryTrigger trigger = startupRound.compareAndSet(true, false)
                ? SignNoRecoveryTrigger.STARTUP
                : SignNoRecoveryTrigger.PERIODIC;
        if (!running.compareAndSet(false, true)) {
            metrics.recordRecovery(trigger, SignMetricConstants.RESULT_SKIPPED);
            log.warn("claim sign no reconciliation skipped trigger={} reason=overlap", trigger.code());
            return;
        }
        String reconciliationId = UUID.randomUUID().toString();
        try {
            LocalDate currentDate = LocalDate.now(clock);
            for (int offset = 0; offset < properties.getReconciliationDays(); offset++) {
                LocalDate businessDate = currentDate.minusDays(offset);
                try {
                    reconcileDate(businessDate, trigger, scopeFor(offset), reconciliationId, currentDate);
                } catch (RuntimeException ignored) {
                    // 单个日期的意外失败已在 reconcileDate 中记录，不阻断其他日期。
                }
            }
        } finally {
            running.set(false);
        }
    }

    private SignNoReconciliationResult manualRound(LocalDate businessDate) {
        if (!running.compareAndSet(false, true)) {
            metrics.recordRecovery(SignNoRecoveryTrigger.MANUAL, SignMetricConstants.RESULT_SKIPPED);
            log.info("claim sign no manual reconciliation skipped date={} reason=overlap", businessDate);
            return SignNoReconciliationResult.skipped(businessDate);
        }
        try {
            LocalDate currentDate = LocalDate.now(clock);
            return reconcileDate(businessDate, SignNoRecoveryTrigger.MANUAL,
                    scopeFor(businessDate, currentDate), UUID.randomUUID().toString(), currentDate);
        } finally {
            running.set(false);
        }
    }

    private SignNoReconciliationResult reconcileDate(LocalDate businessDate, SignNoRecoveryTrigger trigger,
                                                     String scope, String reconciliationId, LocalDate currentDate) {
        SignNoReconciliationResult result;
        try {
            result = reconciliationService.reconcile(businessDate, trigger);
        } catch (RuntimeException exception) {
            metrics.recordRecovery(trigger, SignMetricConstants.RESULT_FAILURE);
            log.error("claim sign no reconciliation failed date={} trigger={} reconciliationId={}",
                    businessDate, trigger.code(), reconciliationId, exception);
            throw exception;
        }
        String outcome = metricOutcome(result.status());
        metrics.recordRecovery(trigger, outcome);
        if (result.status() == ReconciliationStatus.SUCCESS) {
            if (scope != null) {
                metrics.recordSequenceGap(scope, result.sequenceGap());
            }
            if (businessDate.equals(currentDate)) {
                modeCoordinator.markRedisReady();
            }
            log.info("claim sign no reconciled date={} trigger={} result={} dbMax={} before={} after={} "
                            + "gap={} reconciliationId={}", businessDate, trigger.code(), outcome,
                    result.dbHighWaterMark(), result.counterBefore(), result.counterAfter(),
                    result.sequenceGap(), reconciliationId);
        } else {
            if (result.failureReason() != null && result.failureReason().isRedisSide()) {
                modeCoordinator.markRedisUnavailable();
            }
            log.warn("claim sign no reconciliation not applied date={} trigger={} result={} reason={} "
                            + "reconciliationId={}", businessDate, trigger.code(), outcome, result.failureReason(),
                    reconciliationId);
        }
        return result;
    }

    private String metricOutcome(ReconciliationStatus status) {
        if (status == ReconciliationStatus.FAILED) {
            return SignMetricConstants.RESULT_FAILURE;
        }
        if (status == ReconciliationStatus.SUCCESS) {
            return SignMetricConstants.RESULT_SUCCESS;
        }
        return SignMetricConstants.RESULT_SKIPPED;
    }

    private String scopeFor(int offset) {
        if (offset == 0) {
            return SignMetricConstants.SCOPE_CURRENT;
        }
        return offset == 1 ? SignMetricConstants.SCOPE_PREVIOUS : null;
    }

    private String scopeFor(LocalDate businessDate, LocalDate currentDate) {
        if (businessDate.equals(currentDate)) {
            return SignMetricConstants.SCOPE_CURRENT;
        }
        return businessDate.equals(currentDate.minusDays(1))
                ? SignMetricConstants.SCOPE_PREVIOUS
                : null;
    }
}
