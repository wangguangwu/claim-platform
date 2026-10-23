package com.claim.platform.sign.application.service;

import com.claim.platform.sign.application.command.SubmitIntakeCommand;
import com.claim.platform.sign.application.command.UploadMaterialsCommand;
import com.claim.platform.sign.application.exception.IntakeSubmissionInProgressException;
import com.claim.platform.sign.application.exception.SignNoUnavailableException;
import com.claim.platform.sign.application.exception.SignNoUnavailableReason;
import com.claim.platform.sign.application.port.IntakeSubmissionLock;
import com.claim.platform.sign.application.port.IntakeSubmissionLockHandle;
import com.claim.platform.sign.application.port.IntakeSubmissionReplayCache;
import com.claim.platform.sign.application.port.LockAcquireStatus;
import com.claim.platform.sign.application.port.SignNoPolicy;
import com.claim.platform.sign.application.result.ClaimIntakeResult;
import com.claim.platform.sign.application.result.SignNoAllocation;
import com.claim.platform.sign.application.result.SignNoSource;
import com.claim.platform.sign.domain.exception.ClaimIntakeNotFoundException;
import com.claim.platform.sign.domain.model.ClaimIntake;
import com.claim.platform.sign.domain.model.InteractionType;
import com.claim.platform.sign.domain.model.SignNoFormat;
import com.claim.platform.sign.domain.repository.ClaimIntakeStore;
import com.claim.platform.sign.domain.repository.IntakeMaterialStore;
import com.claim.platform.sign.domain.repository.InteractionLogStore;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 在 Claim Intake 限界上下文编排签收提交和材料登记。
 * 签收提交本身不开启数据库事务：快速查询、业务锁、锁内复查都在事务外执行，
 * 只有创建动作进入 {@link ClaimIntakeCreationService} 的独立事务。
 *
 * @author wangguangwu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimIntakeCommandService {

    private static final long BACKOFF_MIN_MILLIS = 10;
    private static final long BACKOFF_MAX_MILLIS_EXCLUSIVE = 31;

    private final ClaimIntakeStore intakeStore;
    private final IntakeMaterialStore materialStore;
    private final InteractionLogStore interactionLogStore;
    private final IntakeSubmissionLock submissionLock;
    private final IntakeSubmissionReplayCache replayCache;
    private final ClaimIntakeCreationService creationService;
    private final SignNoAllocationService signNoAllocationService;
    private final SignNoPolicy signNoPolicy;
    private final SignNoMetrics signNoMetrics;
    private final MeterRegistry meterRegistry;

    /**
     * 接收标准签收命令并按三级幂等创建或回放申请。
     * 幂等键为 {@code tenantCode + externalApplyNo}；锁等待超时返回可重试的并发失败。
     *
     * @param command 已从受信任调用链取得租户的标准命令
     * @return 首次创建或回放的受理结果
     */
    public ClaimIntakeResult submit(SubmitIntakeCommand command) {
        try {
            return submitWithIdempotencyGuard(command);
        } catch (IntakeSubmissionInProgressException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            recordSubmission(IntakeMetricConstants.RESULT_ERROR);
            log.error("claim intake submission failed tenantCode={} traceId={} result={}",
                    command.tenantCode(), command.traceId(), IntakeMetricConstants.RESULT_ERROR, exception);
            throw exception;
        }
    }

    /**
     * 登记材料并将允许补材的受理切换至材料已上传状态。
     *
     * @param command 已按租户和签收编号定位的材料命令
     * @return 材料登记后的受理结果
     */
    @Transactional
    public ClaimIntakeResult uploadMaterials(UploadMaterialsCommand command) {
        ClaimIntake intake = intakeStore.findByTenantAndSignNo(command.tenantCode(), command.signNo())
                .orElseThrow(ClaimIntakeNotFoundException::new);
        intake.registerMaterials();
        materialStore.saveAll(command.tenantCode(), command.signNo(), command.materials());
        intakeStore.save(intake);
        interactionLogStore.recordSuccess(command.tenantCode(), command.signNo(),
                InteractionType.IMAGE_UPLOAD.code(), null, command.traceId());
        return new ClaimIntakeResult(command.signNo(), intake.status());
    }

    private ClaimIntakeResult submitWithIdempotencyGuard(SubmitIntakeCommand command) {
        Optional<ClaimIntake> existing = findByExternalApplyNo(command);
        if (existing.isPresent()) {
            return replay(existing.get(), command,
                    IntakeMetricConstants.LAYER_QUICK_QUERY, IntakeMetricConstants.RESULT_REPLAYED_QUICK);
        }

        try (IntakeSubmissionLockHandle handle = submissionLock.tryAcquire(
                command.tenantCode(), command.externalApplyNo())) {
            if (handle.status() == LockAcquireStatus.TIMEOUT) {
                recordSubmission(IntakeMetricConstants.RESULT_LOCK_TIMEOUT);
                log.warn("claim intake submission lock timeout tenantCode={} traceId={} result={}",
                        command.tenantCode(), command.traceId(), IntakeMetricConstants.RESULT_LOCK_TIMEOUT);
                throw new IntakeSubmissionInProgressException();
            }
            if (handle.status() == LockAcquireStatus.UNAVAILABLE) {
                recordSubmission(IntakeMetricConstants.RESULT_LOCK_DEGRADED);
                log.warn("claim intake submission lock degraded tenantCode={} traceId={} result={}",
                        command.tenantCode(), command.traceId(), IntakeMetricConstants.RESULT_LOCK_DEGRADED);
            }

            existing = findByExternalApplyNo(command);
            return existing.map(claimIntake -> replay(claimIntake, command,
                            IntakeMetricConstants.LAYER_LOCKED_DOUBLE_CHECK,
                            IntakeMetricConstants.RESULT_REPLAYED_LOCKED))
                    .orElseGet(() -> createWithSignNoRetry(command));
        }
    }

    private ClaimIntakeResult createWithSignNoRetry(SubmitIntakeCommand command) {
        int maxAttempts = signNoPolicy.getMaxAttempts();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            SignNoAllocation allocation = signNoAllocationService.next();
            try {
                return create(command, allocation);
            } catch (DuplicateKeyException exception) {
                Optional<ClaimIntake> concurrent = findByExternalApplyNo(command);
                if (concurrent.isPresent()) {
                    signNoMetrics.recordConflict(IntakeMetricConstants.CONFLICT_EXTERNAL_APPLY_NO);
                    return replay(concurrent.get(), command,
                            IntakeMetricConstants.LAYER_UNIQUE_CONFLICT,
                            IntakeMetricConstants.RESULT_REPLAYED_CONFLICT);
                }
                signNoMetrics.recordConflict(IntakeMetricConstants.CONFLICT_SIGN_NO);
                if (allocation.source() == SignNoSource.MYSQL_FALLBACK) {
                    signNoMetrics.recordFallback(IntakeMetricConstants.RESULT_CONFLICT);
                }
                log.warn("claim intake signNo collision tenantCode={} signNo={} traceId={} attempt={} source={}",
                        command.tenantCode(), allocation.signNo(), command.traceId(), attempt, allocation.source());
                if (attempt == maxAttempts) {
                    log.error("claim intake sign no retry exhausted tenantCode={} traceId={} attempts={}",
                            command.tenantCode(), command.traceId(), maxAttempts);
                    throw new SignNoUnavailableException(SignNoUnavailableReason.RETRY_EXHAUSTED, exception);
                }
                signNoAllocationService.recoverAfterConflict(SignNoFormat.parseDate(allocation.signNo()),
                        allocation.source());
                backoff();
            }
        }
        throw new IllegalStateException("sign no retry loop exhausted without terminal branch");
    }

    private ClaimIntakeResult create(SubmitIntakeCommand command, SignNoAllocation allocation) {
        ClaimIntakeResult result = creationService.create(command, allocation.signNo());
        replayCache.put(command.tenantCode(), command.externalApplyNo(), result.signNo());
        recordSubmission(IntakeMetricConstants.RESULT_CREATED);
        log.info("claim intake created tenantCode={} signNo={} source={} traceId={} result={}",
                command.tenantCode(), allocation.signNo(), allocation.source(), command.traceId(),
                IntakeMetricConstants.RESULT_CREATED);
        return result;
    }

    private void backoff() {
        long delayMillis = ThreadLocalRandom.current().nextLong(BACKOFF_MIN_MILLIS, BACKOFF_MAX_MILLIS_EXCLUSIVE);
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SignNoUnavailableException(SignNoUnavailableReason.RETRY_EXHAUSTED, exception);
        }
    }

    private Optional<ClaimIntake> findByExternalApplyNo(SubmitIntakeCommand command) {
        Optional<String> cachedSignNo = replayCache.findSignNo(command.tenantCode(), command.externalApplyNo());
        if (cachedSignNo.isPresent()) {
            Optional<ClaimIntake> cachedIntake = intakeStore.findByTenantAndSignNo(command.tenantCode(),
                    cachedSignNo.get());
            if (cachedIntake.isPresent()) {
                return cachedIntake;
            }
            replayCache.evict(command.tenantCode(), command.externalApplyNo());
        }

        Optional<ClaimIntake> persistedIntake = intakeStore.findByTenantAndExternalApplyNo(command.tenantCode(),
                command.externalApplyNo());
        persistedIntake.ifPresent(intake -> replayCache.put(command.tenantCode(), command.externalApplyNo(),
                intake.signNo()));
        return persistedIntake;
    }

    private ClaimIntakeResult replay(ClaimIntake intake, SubmitIntakeCommand command, String layer, String result) {
        recordSubmission(result);
        log.debug("claim intake replayed tenantCode={} signNo={} traceId={} layer={} result={}",
                command.tenantCode(), intake.signNo(), command.traceId(), layer, result);
        return new ClaimIntakeResult(intake.signNo(), intake.status());
    }

    private void recordSubmission(String result) {
        meterRegistry.counter(IntakeMetricConstants.SUBMISSION_METRIC,
                IntakeMetricConstants.TAG_RESULT, result).increment();
    }
}
