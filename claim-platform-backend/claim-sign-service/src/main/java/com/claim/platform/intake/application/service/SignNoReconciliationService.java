package com.claim.platform.intake.application.service;

import com.claim.platform.intake.application.exception.SignNoSequenceExhaustedException;
import com.claim.platform.intake.application.exception.SignNoUnavailableException;
import com.claim.platform.intake.application.exception.SignNoUnavailableReason;
import com.claim.platform.intake.application.port.LockAcquireStatus;
import com.claim.platform.intake.application.port.SignNoHighWaterMark;
import com.claim.platform.intake.application.port.SignNoRecoveryLock;
import com.claim.platform.intake.application.port.SignNoRecoveryLockHandle;
import com.claim.platform.intake.application.port.SignNoSequenceCounter;
import com.claim.platform.intake.application.result.SignNoReconcileAllocationOutcome;
import com.claim.platform.intake.application.result.SignNoReconcileOutcome;
import com.claim.platform.intake.application.result.SignNoReconciliationResult;
import com.claim.platform.intake.application.result.SignNoRecoveryTrigger;
import com.claim.platform.intake.domain.model.SignNoFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 在按业务日期隔离的恢复锁内使用 MySQL 主库高水位校准 Redis 计数器。
 * 请求路径复用同一服务完成“校准并立即发号”，后台校准只更新计数器、不消耗流水号。
 *
 * @author wangguangwu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignNoReconciliationService {

    private final SignNoRecoveryLock recoveryLock;
    private final SignNoHighWaterMark highWaterMark;
    private final SignNoSequenceCounter counter;

    /**
     * 获取恢复锁、读取主库高水位并原子校准后分配下一个流水号。
     *
     * @throws SignNoUnavailableException         锁不可用、MySQL 查询失败或 Redis 校准时抛出
     * @throws SignNoSequenceExhaustedException   当日流水号耗尽时抛出
     */
    public SignNoReconcileAllocationOutcome reconcileAndAllocate(LocalDate businessDate,
                                                                 SignNoRecoveryTrigger trigger) {
        try (SignNoRecoveryLockHandle handle = recoveryLock.tryAcquire(businessDate)) {
            requireLock(handle);
            long dbHighWaterMark = highWaterMark.currentMaxSequence(businessDate);
            if (dbHighWaterMark >= SignNoFormat.MAX_SEQUENCE) {
                throw new SignNoSequenceExhaustedException();
            }
            SignNoReconcileAllocationOutcome outcome = counter.reconcileAndIncrement(businessDate, dbHighWaterMark);
            if (outcome.sequence() > SignNoFormat.MAX_SEQUENCE) {
                throw new SignNoSequenceExhaustedException();
            }
            log.info("claim intake sign no recovered date={} trigger={} dbMax={} before={} after={} gap={} "
                            + "sequence={}", businessDate, trigger.code(), dbHighWaterMark,
                    outcome.counterBefore(), outcome.counterAfter(), dbHighWaterMark - outcome.counterBefore(),
                    outcome.sequence());
            return outcome;
        } catch (SignNoUnavailableException exception) {
            log.error("claim intake sign no recovery failed date={} trigger={} reason={}", businessDate,
                    trigger.code(), exception.reason(), exception);
            throw exception;
        } catch (SignNoSequenceExhaustedException exception) {
            log.error("claim intake sign no exhausted during recovery date={} trigger={}", businessDate,
                    trigger.code());
            throw exception;
        }
    }

    /**
     * 获取恢复锁、读取主库高水位并只校准 Redis 计数器，不消耗流水号。
     * 锁竞争、Redis 或 MySQL 失败都转换为结果对象，由调用方决定就绪状态和告警。
     */
    public SignNoReconciliationResult reconcile(LocalDate businessDate, SignNoRecoveryTrigger trigger) {
        try (SignNoRecoveryLockHandle handle = recoveryLock.tryAcquire(businessDate)) {
            if (handle.status() != LockAcquireStatus.ACQUIRED) {
                SignNoUnavailableReason reason = handle.status() == LockAcquireStatus.TIMEOUT
                        ? SignNoUnavailableReason.RECOVERY_LOCK_TIMEOUT
                        : SignNoUnavailableReason.RECOVERY_LOCK_UNAVAILABLE;
                log.info("claim intake sign no reconciliation skipped date={} trigger={} reason={}",
                        businessDate, trigger.code(), reason);
                return SignNoReconciliationResult.skipped(businessDate, reason);
            }
            long dbHighWaterMark = highWaterMark.currentMaxSequence(businessDate);
            SignNoReconcileOutcome outcome = counter.reconcile(businessDate, dbHighWaterMark);
            long gap = dbHighWaterMark - outcome.counterBefore();
            log.info("claim intake sign no reconciled date={} trigger={} dbMax={} before={} after={} gap={} "
                            + "changed={}", businessDate, trigger.code(), dbHighWaterMark,
                    outcome.counterBefore(), outcome.counterAfter(), gap,
                    outcome.counterBefore() != outcome.counterAfter());
            return SignNoReconciliationResult.success(businessDate, dbHighWaterMark,
                    outcome.counterBefore(), outcome.counterAfter());
        } catch (SignNoUnavailableException exception) {
            log.error("claim intake sign no reconciliation failed date={} trigger={} reason={}", businessDate,
                    trigger.code(), exception.reason(), exception);
            return SignNoReconciliationResult.failed(businessDate, exception.reason());
        }
    }

    private void requireLock(SignNoRecoveryLockHandle handle) {
        if (handle.status() == LockAcquireStatus.TIMEOUT) {
            throw new SignNoUnavailableException(SignNoUnavailableReason.RECOVERY_LOCK_TIMEOUT);
        }
        if (handle.status() == LockAcquireStatus.UNAVAILABLE) {
            throw new SignNoUnavailableException(SignNoUnavailableReason.RECOVERY_LOCK_UNAVAILABLE);
        }
    }
}
