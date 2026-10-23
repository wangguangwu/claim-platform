package com.claim.platform.intake.application.service;

import com.claim.platform.intake.application.command.SubmitIntakeCommand;
import com.claim.platform.intake.application.command.UploadMaterialsCommand;
import com.claim.platform.intake.application.exception.IntakeSubmissionInProgressException;
import com.claim.platform.intake.application.port.IntakeSubmissionLock;
import com.claim.platform.intake.application.port.IntakeSubmissionLockHandle;
import com.claim.platform.intake.application.port.IntakeSubmissionReplayCache;
import com.claim.platform.intake.application.port.LockAcquireStatus;
import com.claim.platform.intake.application.result.ClaimIntakeResult;
import com.claim.platform.intake.domain.exception.ClaimIntakeNotFoundException;
import com.claim.platform.intake.domain.model.ClaimIntake;
import com.claim.platform.intake.domain.repository.ClaimIntakeStore;
import com.claim.platform.intake.domain.repository.IntakeMaterialStore;
import com.claim.platform.intake.domain.repository.InteractionLogStore;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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

    private static final String SUBMISSION_METRIC = "claim.intake.submission";
    private static final String CONFLICT_METRIC = "claim.intake.submission.conflict";

    private static final String RESULT_CREATED = "created";
    private static final String RESULT_REPLAYED_QUICK = "replayed_quick";
    private static final String RESULT_REPLAYED_LOCKED = "replayed_locked";
    private static final String RESULT_REPLAYED_CONFLICT = "replayed_conflict";
    private static final String RESULT_LOCK_TIMEOUT = "lock_timeout";
    private static final String RESULT_LOCK_DEGRADED = "lock_degraded";
    private static final String RESULT_ERROR = "error";

    private static final String CONFLICT_EXTERNAL_APPLY_NO = "external_apply_no";
    private static final String CONFLICT_SIGN_NO_RETRY = "sign_no_retry";

    private static final String LAYER_QUICK_QUERY = "quick-query";
    private static final String LAYER_LOCKED_DOUBLE_CHECK = "locked-double-check";
    private static final String LAYER_UNIQUE_CONFLICT = "unique-conflict";

    private static final String INTERACTION_IMAGE_UPLOAD = "IMAGE_UPLOAD";

    private final ClaimIntakeStore intakeStore;
    private final IntakeMaterialStore materialStore;
    private final InteractionLogStore interactionLogStore;
    private final IntakeSubmissionLock submissionLock;
    private final IntakeSubmissionReplayCache replayCache;
    private final ClaimIntakeCreationService creationService;
    private final SignNoGenerator signNoGenerator;
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
            recordSubmission(RESULT_ERROR);
            log.error("claim intake submission failed tenantCode={} traceId={} result={}",
                    command.tenantCode(), command.traceId(), RESULT_ERROR, exception);
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
        interactionLogStore.recordSuccess(command.tenantCode(), command.signNo(), INTERACTION_IMAGE_UPLOAD, null,
                command.traceId());
        return new ClaimIntakeResult(command.signNo(), intake.status());
    }

    private ClaimIntakeResult submitWithIdempotencyGuard(SubmitIntakeCommand command) {
        Optional<ClaimIntake> existing = findByExternalApplyNo(command);
        if (existing.isPresent()) {
            return replay(existing.get(), command, LAYER_QUICK_QUERY, RESULT_REPLAYED_QUICK);
        }

        try (IntakeSubmissionLockHandle handle = submissionLock.tryAcquire(
                command.tenantCode(), command.externalApplyNo())) {
            if (handle.status() == LockAcquireStatus.TIMEOUT) {
                recordSubmission(RESULT_LOCK_TIMEOUT);
                log.warn("claim intake submission lock timeout tenantCode={} traceId={} result={}",
                        command.tenantCode(), command.traceId(), RESULT_LOCK_TIMEOUT);
                throw new IntakeSubmissionInProgressException();
            }
            if (handle.status() == LockAcquireStatus.UNAVAILABLE) {
                recordSubmission(RESULT_LOCK_DEGRADED);
                log.warn("claim intake submission lock degraded tenantCode={} traceId={} result={}",
                        command.tenantCode(), command.traceId(), RESULT_LOCK_DEGRADED);
            }

            existing = findByExternalApplyNo(command);
            if (existing.isPresent()) {
                return replay(existing.get(), command, LAYER_LOCKED_DOUBLE_CHECK, RESULT_REPLAYED_LOCKED);
            }
            return createWithSignNoRetry(command);
        }
    }

    private ClaimIntakeResult createWithSignNoRetry(SubmitIntakeCommand command) {
        try {
            return create(command);
        } catch (DuplicateKeyException exception) {
            Optional<ClaimIntake> concurrent = findByExternalApplyNo(command);
            if (concurrent.isPresent()) {
                recordConflict(CONFLICT_EXTERNAL_APPLY_NO);
                return replay(concurrent.get(), command, LAYER_UNIQUE_CONFLICT, RESULT_REPLAYED_CONFLICT);
            }
            recordConflict(CONFLICT_SIGN_NO_RETRY);
            log.warn("claim intake signNo collision tenantCode={} traceId={} result={}",
                    command.tenantCode(), command.traceId(), CONFLICT_SIGN_NO_RETRY);
            return create(command);
        }
    }

    private ClaimIntakeResult create(SubmitIntakeCommand command) {
        String signNo = signNoGenerator.generate();
        ClaimIntakeResult result = creationService.create(command, signNo);
        replayCache.put(command.tenantCode(), command.externalApplyNo(), result.signNo());
        recordSubmission(RESULT_CREATED);
        log.info("claim intake created tenantCode={} signNo={} traceId={} result={}",
                command.tenantCode(), signNo, command.traceId(), RESULT_CREATED);
        return result;
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
        meterRegistry.counter(SUBMISSION_METRIC, "result", result).increment();
    }

    private void recordConflict(String type) {
        meterRegistry.counter(CONFLICT_METRIC, "type", type).increment();
    }
}
