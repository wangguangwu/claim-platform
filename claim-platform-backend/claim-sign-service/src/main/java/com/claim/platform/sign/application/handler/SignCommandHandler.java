package com.claim.platform.sign.application.handler;

import com.claim.platform.sign.application.command.SignCommand;
import com.claim.platform.sign.application.command.UploadMaterialCommand;
import com.claim.platform.sign.application.exception.SignNoUnavailableException;
import com.claim.platform.sign.application.exception.SignNoUnavailableReason;
import com.claim.platform.sign.application.exception.SignSubmissionInProgressException;
import com.claim.platform.sign.application.port.LockAcquireStatus;
import com.claim.platform.sign.application.port.SignNoPolicy;
import com.claim.platform.sign.application.port.SignSubmissionLock;
import com.claim.platform.sign.application.port.SignSubmissionLockHandle;
import com.claim.platform.sign.application.port.SignSubmissionReplayCache;
import com.claim.platform.sign.application.result.SignNoAllocation;
import com.claim.platform.sign.application.result.SignNoSource;
import com.claim.platform.sign.application.result.SignResult;
import com.claim.platform.sign.application.service.SignCreationService;
import com.claim.platform.sign.application.service.SignMetricConstants;
import com.claim.platform.sign.application.service.SignNoAllocationService;
import com.claim.platform.sign.application.service.SignNoMetrics;
import com.claim.platform.sign.domain.exception.SignNotFoundException;
import com.claim.platform.sign.domain.model.InteractionType;
import com.claim.platform.sign.domain.model.Sign;
import com.claim.platform.sign.domain.model.SignNo;
import com.claim.platform.sign.domain.repository.InteractionLogRepository;
import com.claim.platform.sign.domain.repository.MaterialRepository;
import com.claim.platform.sign.domain.repository.SignRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 在理赔签收限界上下文编排签收提交和材料登记。
 * 签收提交本身不开启数据库事务：快速查询、业务锁、锁内复查都在事务外执行，
 * 只有创建动作进入 {@link SignCreationService} 的独立事务。
 *
 * @author wangguangwu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignCommandHandler {

    private static final long BACKOFF_MIN_MILLIS = 10;
    private static final long BACKOFF_MAX_MILLIS_EXCLUSIVE = 31;

    private final SignRepository signRepository;
    private final MaterialRepository materialRepository;
    private final InteractionLogRepository interactionLogRepository;
    private final SignSubmissionLock submissionLock;
    private final SignSubmissionReplayCache replayCache;
    private final SignCreationService creationService;
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
    public SignResult submit(SignCommand command) {
        try {
            return submitWithIdempotencyGuard(command);
        } catch (SignSubmissionInProgressException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            recordSubmission(SignMetricConstants.RESULT_ERROR);
            log.error("claim sign submission failed tenantCode={} traceId={} result={}",
                    command.tenantCode(), command.traceId(), SignMetricConstants.RESULT_ERROR, exception);
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
    public SignResult uploadMaterials(UploadMaterialCommand command) {
        Sign sign = signRepository.findByTenantAndSignNo(command.tenantCode(), command.signNo())
                .orElseThrow(SignNotFoundException::new);
        sign.registerMaterials();
        materialRepository.saveAll(command.tenantCode(), command.signNo(), command.materials());
        signRepository.save(sign);
        interactionLogRepository.recordSuccess(command.tenantCode(), command.signNo(),
                InteractionType.IMAGE_UPLOAD.code(), null, command.traceId());
        return new SignResult(command.signNo(), sign.status());
    }

    private SignResult submitWithIdempotencyGuard(SignCommand command) {
        Optional<Sign> existing = findByExternalApplyNo(command);
        if (existing.isPresent()) {
            return replay(existing.get(), command,
                    SignMetricConstants.LAYER_QUICK_QUERY, SignMetricConstants.RESULT_REPLAYED_QUICK);
        }

        try (SignSubmissionLockHandle handle = submissionLock.tryAcquire(
                command.tenantCode(), command.externalApplyNo())) {
            if (handle.status() == LockAcquireStatus.TIMEOUT) {
                recordSubmission(SignMetricConstants.RESULT_LOCK_TIMEOUT);
                log.warn("claim sign submission lock timeout tenantCode={} traceId={} result={}",
                        command.tenantCode(), command.traceId(), SignMetricConstants.RESULT_LOCK_TIMEOUT);
                throw new SignSubmissionInProgressException();
            }
            if (handle.status() == LockAcquireStatus.UNAVAILABLE) {
                recordSubmission(SignMetricConstants.RESULT_LOCK_DEGRADED);
                log.warn("claim sign submission lock degraded tenantCode={} traceId={} result={}",
                        command.tenantCode(), command.traceId(), SignMetricConstants.RESULT_LOCK_DEGRADED);
            }

            existing = findByExternalApplyNo(command);
            return existing.map(sign -> replay(sign, command,
                            SignMetricConstants.LAYER_LOCKED_DOUBLE_CHECK,
                            SignMetricConstants.RESULT_REPLAYED_LOCKED))
                    .orElseGet(() -> createWithSignNoRetry(command));
        }
    }

    private SignResult createWithSignNoRetry(SignCommand command) {
        int maxAttempts = signNoPolicy.getMaxAttempts();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            SignNoAllocation allocation = signNoAllocationService.next();
            try {
                return create(command, allocation);
            } catch (DuplicateKeyException exception) {
                Optional<Sign> concurrent = findByExternalApplyNo(command);
                if (concurrent.isPresent()) {
                    signNoMetrics.recordConflict(SignMetricConstants.CONFLICT_EXTERNAL_APPLY_NO);
                    return replay(concurrent.get(), command,
                            SignMetricConstants.LAYER_UNIQUE_CONFLICT,
                            SignMetricConstants.RESULT_REPLAYED_CONFLICT);
                }
                signNoMetrics.recordConflict(SignMetricConstants.CONFLICT_SIGN_NO);
                if (allocation.source() == SignNoSource.MYSQL_FALLBACK) {
                    signNoMetrics.recordFallback(SignMetricConstants.RESULT_CONFLICT);
                }
                log.warn("claim sign signNo collision tenantCode={} signNo={} traceId={} attempt={} source={}",
                        command.tenantCode(), allocation.signNo(), command.traceId(), attempt, allocation.source());
                if (attempt == maxAttempts) {
                    log.error("claim sign no retry exhausted tenantCode={} traceId={} attempts={}",
                            command.tenantCode(), command.traceId(), maxAttempts);
                    throw new SignNoUnavailableException(SignNoUnavailableReason.RETRY_EXHAUSTED, exception);
                }
                signNoAllocationService.recoverAfterConflict(SignNo.parseDate(allocation.signNo()),
                        allocation.source());
                backoff();
            }
        }
        throw new IllegalStateException("sign no retry loop exhausted without terminal branch");
    }

    private SignResult create(SignCommand command, SignNoAllocation allocation) {
        SignResult result = creationService.create(command, allocation.signNo());
        replayCache.put(command.tenantCode(), command.externalApplyNo(), result.signNo());
        recordSubmission(SignMetricConstants.RESULT_CREATED);
        log.info("claim sign created tenantCode={} signNo={} source={} traceId={} result={}",
                command.tenantCode(), allocation.signNo(), allocation.source(), command.traceId(),
                SignMetricConstants.RESULT_CREATED);
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

    private Optional<Sign> findByExternalApplyNo(SignCommand command) {
        Optional<String> cachedSignNo = replayCache.findSignNo(command.tenantCode(), command.externalApplyNo());
        if (cachedSignNo.isPresent()) {
            Optional<Sign> cachedSign = signRepository.findByTenantAndSignNo(command.tenantCode(),
                    cachedSignNo.get());
            if (cachedSign.isPresent()) {
                return cachedSign;
            }
            replayCache.evict(command.tenantCode(), command.externalApplyNo());
        }

        Optional<Sign> persistedSign = signRepository.findByTenantAndExternalApplyNo(command.tenantCode(),
                command.externalApplyNo());
        persistedSign.ifPresent(sign -> replayCache.put(command.tenantCode(), command.externalApplyNo(),
                sign.signNo()));
        return persistedSign;
    }

    private SignResult replay(Sign sign, SignCommand command, String layer, String result) {
        recordSubmission(result);
        log.debug("claim sign replayed tenantCode={} signNo={} traceId={} layer={} result={}",
                command.tenantCode(), sign.signNo(), command.traceId(), layer, result);
        return new SignResult(sign.signNo(), sign.status());
    }

    private void recordSubmission(String result) {
        meterRegistry.counter(SignMetricConstants.SUBMISSION_METRIC,
                SignMetricConstants.TAG_RESULT, result).increment();
    }
}
