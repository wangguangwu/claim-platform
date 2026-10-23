package com.claim.platform.intake.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.claim.platform.intake.application.exception.SignNoSequenceExhaustedException;
import com.claim.platform.intake.application.exception.SignNoUnavailableException;
import com.claim.platform.intake.application.exception.SignNoUnavailableReason;
import com.claim.platform.intake.application.port.LockAcquireStatus;
import com.claim.platform.intake.application.port.SignNoHighWaterMark;
import com.claim.platform.intake.application.port.SignNoRecoveryLock;
import com.claim.platform.intake.application.port.SignNoRecoveryLockHandle;
import com.claim.platform.intake.application.port.SignNoSequenceCounter;
import com.claim.platform.intake.application.result.ReconciliationStatus;
import com.claim.platform.intake.application.result.SignNoReconcileAllocationOutcome;
import com.claim.platform.intake.application.result.SignNoReconcileOutcome;
import com.claim.platform.intake.application.result.SignNoReconciliationResult;
import com.claim.platform.intake.application.result.SignNoRecoveryTrigger;
import com.claim.platform.intake.domain.model.SignNoFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

/**
 * 验证恢复锁、MySQL 高水位和 Redis 校准组合出的请求恢复与后台校准语义。
 *
 * @author wangguangwu
 */
@ExtendWith(MockitoExtension.class)
class SignNoReconciliationServiceTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 10, 23);

    @Mock
    private SignNoRecoveryLock recoveryLock;
    @Mock
    private SignNoHighWaterMark highWaterMark;
    @Mock
    private SignNoSequenceCounter counter;

    private SignNoReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        reconciliationService = new SignNoReconciliationService(recoveryLock, highWaterMark, counter);
    }

    @Test
    void shouldReconcileAndAllocateInsideRecoveryLock() {
        SignNoRecoveryLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        given(recoveryLock.tryAcquire(BUSINESS_DATE)).willReturn(handle);
        given(highWaterMark.currentMaxSequence(BUSINESS_DATE)).willReturn(10L);
        given(counter.reconcileAndIncrement(BUSINESS_DATE, 10L))
                .willReturn(new SignNoReconcileAllocationOutcome(11, 10, 11));

        SignNoReconcileAllocationOutcome outcome =
                reconciliationService.reconcileAndAllocate(BUSINESS_DATE, SignNoRecoveryTrigger.MISSING_COUNTER);

        assertThat(outcome.sequence()).isEqualTo(11);
        assertThat(outcome.counterBefore()).isEqualTo(10);
        assertThat(outcome.counterAfter()).isEqualTo(11);
        verify(handle).close();
    }

    @Test
    void shouldFailRecoveryAllocationWhenLockTimeout() {
        SignNoRecoveryLockHandle handle = handle(LockAcquireStatus.TIMEOUT);
        given(recoveryLock.tryAcquire(BUSINESS_DATE)).willReturn(handle);

        assertThatThrownBy(() -> reconciliationService.reconcileAndAllocate(BUSINESS_DATE,
                SignNoRecoveryTrigger.MISSING_COUNTER))
                .isInstanceOf(SignNoUnavailableException.class)
                .satisfies(exception -> assertThat(((SignNoUnavailableException) exception).reason())
                        .isEqualTo(SignNoUnavailableReason.RECOVERY_LOCK_TIMEOUT));

        verifyNoInteractions(highWaterMark, counter);
        verify(handle).close();
    }

    @Test
    void shouldFailRecoveryAllocationWhenLockUnavailable() {
        SignNoRecoveryLockHandle handle = handle(LockAcquireStatus.UNAVAILABLE);
        given(recoveryLock.tryAcquire(BUSINESS_DATE)).willReturn(handle);

        assertThatThrownBy(() -> reconciliationService.reconcileAndAllocate(BUSINESS_DATE,
                SignNoRecoveryTrigger.INVALID_COUNTER))
                .isInstanceOf(SignNoUnavailableException.class)
                .satisfies(exception -> assertThat(((SignNoUnavailableException) exception).reason())
                        .isEqualTo(SignNoUnavailableReason.RECOVERY_LOCK_UNAVAILABLE));

        verifyNoInteractions(highWaterMark, counter);
        verify(handle).close();
    }

    @Test
    void shouldPropagateHighWaterMarkFailureDuringRecoveryAllocation() {
        SignNoRecoveryLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        given(recoveryLock.tryAcquire(BUSINESS_DATE)).willReturn(handle);
        SignNoUnavailableException failure =
                new SignNoUnavailableException(SignNoUnavailableReason.HIGH_WATER_MARK_UNAVAILABLE);
        given(highWaterMark.currentMaxSequence(BUSINESS_DATE)).willThrow(failure);

        assertThatThrownBy(() -> reconciliationService.reconcileAndAllocate(BUSINESS_DATE,
                SignNoRecoveryTrigger.MISSING_COUNTER)).isSameAs(failure);

        verifyNoInteractions(counter);
        verify(handle).close();
    }

    @Test
    void shouldFailRecoveryAllocationWhenHighWaterMarkExhausted() {
        SignNoRecoveryLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        given(recoveryLock.tryAcquire(BUSINESS_DATE)).willReturn(handle);
        given(highWaterMark.currentMaxSequence(BUSINESS_DATE)).willReturn(SignNoFormat.MAX_SEQUENCE);

        assertThatThrownBy(() -> reconciliationService.reconcileAndAllocate(BUSINESS_DATE,
                SignNoRecoveryTrigger.MISSING_COUNTER))
                .isInstanceOf(SignNoSequenceExhaustedException.class);

        verifyNoInteractions(counter);
        verify(handle).close();
    }

    @Test
    void shouldFailRecoveryAllocationWhenCounterReturnsOutOfRangeSequence() {
        SignNoRecoveryLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        given(recoveryLock.tryAcquire(BUSINESS_DATE)).willReturn(handle);
        given(highWaterMark.currentMaxSequence(BUSINESS_DATE)).willReturn(10L);
        given(counter.reconcileAndIncrement(BUSINESS_DATE, 10L))
                .willReturn(new SignNoReconcileAllocationOutcome(SignNoFormat.MAX_SEQUENCE + 1, 10,
                        SignNoFormat.MAX_SEQUENCE));

        assertThatThrownBy(() -> reconciliationService.reconcileAndAllocate(BUSINESS_DATE,
                SignNoRecoveryTrigger.MISSING_COUNTER))
                .isInstanceOf(SignNoSequenceExhaustedException.class);

        verify(handle).close();
    }

    @Test
    void shouldReconcileWithoutConsumingSequence() {
        SignNoRecoveryLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        given(recoveryLock.tryAcquire(BUSINESS_DATE)).willReturn(handle);
        given(highWaterMark.currentMaxSequence(BUSINESS_DATE)).willReturn(10L);
        given(counter.reconcile(BUSINESS_DATE, 10L)).willReturn(new SignNoReconcileOutcome(7, 10));

        SignNoReconciliationResult result =
                reconciliationService.reconcile(BUSINESS_DATE, SignNoRecoveryTrigger.PERIODIC);

        assertThat(result.status()).isEqualTo(ReconciliationStatus.SUCCESS);
        assertThat(result.dbHighWaterMark()).isEqualTo(10);
        assertThat(result.counterBefore()).isEqualTo(7);
        assertThat(result.counterAfter()).isEqualTo(10);
        assertThat(result.changed()).isTrue();
        assertThat(result.sequenceGap()).isEqualTo(3);
        assertThat(result.failureReason()).isNull();
        verify(handle).close();
    }

    @Test
    void shouldSkipReconcileWhenLockTimeout() {
        SignNoRecoveryLockHandle handle = handle(LockAcquireStatus.TIMEOUT);
        given(recoveryLock.tryAcquire(BUSINESS_DATE)).willReturn(handle);

        SignNoReconciliationResult result =
                reconciliationService.reconcile(BUSINESS_DATE, SignNoRecoveryTrigger.PERIODIC);

        assertThat(result.status()).isEqualTo(ReconciliationStatus.SKIPPED);
        assertThat(result.failureReason()).isEqualTo(SignNoUnavailableReason.RECOVERY_LOCK_TIMEOUT);
        assertThat(result.dbHighWaterMark()).isEqualTo(-1);
        verifyNoInteractions(highWaterMark, counter);
        verify(handle).close();
    }

    @Test
    void shouldSkipReconcileWhenLockUnavailable() {
        SignNoRecoveryLockHandle handle = handle(LockAcquireStatus.UNAVAILABLE);
        given(recoveryLock.tryAcquire(BUSINESS_DATE)).willReturn(handle);

        SignNoReconciliationResult result =
                reconciliationService.reconcile(BUSINESS_DATE, SignNoRecoveryTrigger.STARTUP);

        assertThat(result.status()).isEqualTo(ReconciliationStatus.SKIPPED);
        assertThat(result.failureReason()).isEqualTo(SignNoUnavailableReason.RECOVERY_LOCK_UNAVAILABLE);
        verifyNoInteractions(highWaterMark, counter);
        verify(handle).close();
    }

    @Test
    void shouldReturnFailedResultWhenRedisCalibrationFails() {
        SignNoRecoveryLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        given(recoveryLock.tryAcquire(BUSINESS_DATE)).willReturn(handle);
        given(highWaterMark.currentMaxSequence(BUSINESS_DATE)).willReturn(10L);
        given(counter.reconcile(BUSINESS_DATE, 10L))
                .willThrow(new SignNoUnavailableException(SignNoUnavailableReason.REDIS_UNAVAILABLE));

        SignNoReconciliationResult result =
                reconciliationService.reconcile(BUSINESS_DATE, SignNoRecoveryTrigger.PERIODIC);

        assertThat(result.status()).isEqualTo(ReconciliationStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo(SignNoUnavailableReason.REDIS_UNAVAILABLE);
        assertThat(result.sequenceGap()).isEqualTo(-1);
        verify(handle).close();
    }

    @Test
    void shouldReturnFailedResultWhenHighWaterMarkFails() {
        SignNoRecoveryLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        given(recoveryLock.tryAcquire(BUSINESS_DATE)).willReturn(handle);
        given(highWaterMark.currentMaxSequence(BUSINESS_DATE))
                .willThrow(new SignNoUnavailableException(SignNoUnavailableReason.INVALID_HIGH_WATER_MARK));

        SignNoReconciliationResult result =
                reconciliationService.reconcile(BUSINESS_DATE, SignNoRecoveryTrigger.PERIODIC);

        assertThat(result.status()).isEqualTo(ReconciliationStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo(SignNoUnavailableReason.INVALID_HIGH_WATER_MARK);
        verifyNoInteractions(counter);
        verify(handle).close();
    }

    private SignNoRecoveryLockHandle handle(LockAcquireStatus status) {
        SignNoRecoveryLockHandle handle = mock(SignNoRecoveryLockHandle.class);
        given(handle.status()).willReturn(status);
        return handle;
    }
}
