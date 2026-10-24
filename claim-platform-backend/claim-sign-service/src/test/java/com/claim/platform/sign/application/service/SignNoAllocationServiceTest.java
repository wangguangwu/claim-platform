package com.claim.platform.sign.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.claim.platform.sign.application.exception.SignNoSequenceExhaustedException;
import com.claim.platform.sign.application.exception.SignNoUnavailableException;
import com.claim.platform.sign.application.exception.SignNoUnavailableReason;
import com.claim.platform.sign.application.port.SignNoFallbackAllocator;
import com.claim.platform.sign.application.port.SignNoIncrementResult;
import com.claim.platform.sign.application.port.SignNoPolicy;
import com.claim.platform.sign.application.port.SignNoSequenceCounter;
import com.claim.platform.sign.application.result.SignNoAllocation;
import com.claim.platform.sign.application.result.SignNoReconcileAllocationOutcome;
import com.claim.platform.sign.application.result.SignNoReconciliationResult;
import com.claim.platform.sign.application.result.SignNoRecoveryTrigger;
import com.claim.platform.sign.application.result.SignNoSource;
import com.claim.platform.sign.domain.model.SignNo;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 验证 Redis 主路径、缺失/非法计数器恢复、Redis 故障降级和冲突后校准的来源选择。
 *
 * @author wangguangwu
 */
@ExtendWith(MockitoExtension.class)
class SignNoAllocationServiceTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 10, 23);
    private static final String ALLOCATION_METRIC = "claim.intake.number.allocation";
    private static final String FALLBACK_METRIC = "claim.intake.number.fallback";
    private static final String RECOVERY_METRIC = "claim.intake.number.recovery";

    @Mock
    private SignNoSequenceCounter counter;
    @Mock
    private SignNoReconciliationService reconciliationService;
    @Mock
    private SignNoFallbackAllocator fallbackAllocator;
    @Mock
    private SignNoModeCoordinator modeCoordinator;
    @Mock
    private SignNoPolicy signNoPolicy;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private SignNoAllocationService allocationService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-23T02:00:00Z"), ZoneId.of("Asia/Shanghai"));
        allocationService = new SignNoAllocationService(counter, reconciliationService, fallbackAllocator,
                modeCoordinator, signNoPolicy, new SignNoMetrics(meterRegistry), clock);
    }

    @Test
    void shouldAllocateFromRedisWhenReady() {
        given(modeCoordinator.isRedisReady()).willReturn(true);
        given(counter.tryIncrement(BUSINESS_DATE)).willReturn(new SignNoIncrementResult.Allocated(7));

        SignNoAllocation allocation = allocationService.next();

        assertThat(allocation.signNo()).isEqualTo(SignNo.format(BUSINESS_DATE, 7));
        assertThat(allocation.source()).isEqualTo(SignNoSource.REDIS);
        assertThat(allocationCount("redis", "allocated")).isEqualTo(1.0);
        verifyNoInteractions(fallbackAllocator, reconciliationService);
    }

    @Test
    void shouldUseFallbackWhenInstanceNotReady() {
        given(signNoPolicy.isFallbackEnabled()).willReturn(true);
        given(fallbackAllocator.nextSequence(BUSINESS_DATE)).willReturn(5L);

        SignNoAllocation allocation = allocationService.next();

        assertThat(allocation.signNo()).isEqualTo(SignNo.format(BUSINESS_DATE, 5));
        assertThat(allocation.source()).isEqualTo(SignNoSource.MYSQL_FALLBACK);
        assertThat(fallbackCount("success")).isEqualTo(1.0);
        assertThat(allocationCount("mysql_fallback", "allocated")).isEqualTo(1.0);
        verifyNoInteractions(counter, reconciliationService);
    }

    @Test
    void shouldRejectFallbackWhenDisabled() {
        assertThatThrownBy(() -> allocationService.next())
                .isInstanceOf(SignNoUnavailableException.class)
                .satisfies(exception -> assertThat(((SignNoUnavailableException) exception).reason())
                        .isEqualTo(SignNoUnavailableReason.FALLBACK_DISABLED));

        verifyNoInteractions(fallbackAllocator, counter, reconciliationService);
    }

    @Test
    void shouldRecoverAndAllocateWhenCounterMissing() {
        given(modeCoordinator.isRedisReady()).willReturn(true);
        given(counter.tryIncrement(BUSINESS_DATE)).willReturn(SignNoIncrementResult.Missing.INSTANCE);
        given(reconciliationService.reconcileAndAllocate(BUSINESS_DATE,
                SignNoRecoveryTrigger.MISSING_COUNTER))
                .willReturn(new SignNoReconcileAllocationOutcome(9, 4, 9));

        SignNoAllocation allocation = allocationService.next();

        assertThat(allocation.signNo()).isEqualTo(SignNo.format(BUSINESS_DATE, 9));
        assertThat(allocation.source()).isEqualTo(SignNoSource.REDIS);
        assertThat(recoveryCount("missing_counter", "success")).isEqualTo(1.0);
        assertThat(allocationCount("redis", "recovered")).isEqualTo(1.0);
        verifyNoInteractions(fallbackAllocator);
    }

    @Test
    void shouldRecoverAndAllocateWhenCounterInvalid() {
        given(modeCoordinator.isRedisReady()).willReturn(true);
        given(counter.tryIncrement(BUSINESS_DATE)).willReturn(SignNoIncrementResult.Invalid.INSTANCE);
        given(reconciliationService.reconcileAndAllocate(BUSINESS_DATE,
                SignNoRecoveryTrigger.INVALID_COUNTER))
                .willReturn(new SignNoReconcileAllocationOutcome(3, 0, 3));

        SignNoAllocation allocation = allocationService.next();

        assertThat(allocation.signNo()).isEqualTo(SignNo.format(BUSINESS_DATE, 3));
        assertThat(recoveryCount("invalid_counter", "success")).isEqualTo(1.0);
        assertThat(allocationCount("redis", "recovered")).isEqualTo(1.0);
    }

    @Test
    void shouldMarkUnavailableAndFallbackWhenRedisFails() {
        given(modeCoordinator.isRedisReady()).willReturn(true);
        given(counter.tryIncrement(BUSINESS_DATE))
                .willThrow(new SignNoUnavailableException(SignNoUnavailableReason.REDIS_UNAVAILABLE));
        given(signNoPolicy.isFallbackEnabled()).willReturn(true);
        given(fallbackAllocator.nextSequence(BUSINESS_DATE)).willReturn(5L);

        SignNoAllocation allocation = allocationService.next();

        verify(modeCoordinator).markRedisUnavailable();
        assertThat(allocation.source()).isEqualTo(SignNoSource.MYSQL_FALLBACK);
        assertThat(allocationCount("redis", "unavailable")).isEqualTo(1.0);
        assertThat(fallbackCount("success")).isEqualTo(1.0);
    }

    @Test
    void shouldPropagateMysqlSideRecoveryFailureWithoutFallback() {
        given(modeCoordinator.isRedisReady()).willReturn(true);
        given(counter.tryIncrement(BUSINESS_DATE)).willReturn(SignNoIncrementResult.Missing.INSTANCE);
        SignNoUnavailableException failure =
                new SignNoUnavailableException(SignNoUnavailableReason.HIGH_WATER_MARK_UNAVAILABLE);
        given(reconciliationService.reconcileAndAllocate(BUSINESS_DATE,
                SignNoRecoveryTrigger.MISSING_COUNTER)).willThrow(failure);

        assertThatThrownBy(() -> allocationService.next()).isSameAs(failure);

        verify(modeCoordinator, never()).markRedisUnavailable();
        verifyNoInteractions(fallbackAllocator);
        assertThat(recoveryCount("missing_counter", "failure")).isEqualTo(1.0);
        assertThat(allocationCount("redis", "unavailable")).isEqualTo(1.0);
    }

    @Test
    void shouldRetryCounterOnceAfterRecoveryLockTimeout() {
        given(modeCoordinator.isRedisReady()).willReturn(true);
        given(counter.tryIncrement(BUSINESS_DATE)).willReturn(
                SignNoIncrementResult.Missing.INSTANCE,
                new SignNoIncrementResult.Allocated(6));
        given(reconciliationService.reconcileAndAllocate(BUSINESS_DATE,
                SignNoRecoveryTrigger.MISSING_COUNTER))
                .willThrow(new SignNoUnavailableException(SignNoUnavailableReason.RECOVERY_LOCK_TIMEOUT));

        SignNoAllocation allocation = allocationService.next();

        assertThat(allocation.signNo()).isEqualTo(SignNo.format(BUSINESS_DATE, 6));
        assertThat(allocation.source()).isEqualTo(SignNoSource.REDIS);
        assertThat(recoveryCount("missing_counter", "success")).isEqualTo(1.0);
        verify(modeCoordinator, never()).markRedisUnavailable();
        verifyNoInteractions(fallbackAllocator);
    }

    @Test
    void shouldFallbackWhenRecoveryLockTimeoutAndRetryFails() {
        given(modeCoordinator.isRedisReady()).willReturn(true);
        given(counter.tryIncrement(BUSINESS_DATE)).willReturn(
                SignNoIncrementResult.Missing.INSTANCE,
                SignNoIncrementResult.Invalid.INSTANCE);
        given(reconciliationService.reconcileAndAllocate(BUSINESS_DATE,
                SignNoRecoveryTrigger.MISSING_COUNTER))
                .willThrow(new SignNoUnavailableException(SignNoUnavailableReason.RECOVERY_LOCK_TIMEOUT));
        given(signNoPolicy.isFallbackEnabled()).willReturn(true);
        given(fallbackAllocator.nextSequence(BUSINESS_DATE)).willReturn(8L);

        SignNoAllocation allocation = allocationService.next();

        assertThat(allocation.source()).isEqualTo(SignNoSource.MYSQL_FALLBACK);
        verify(modeCoordinator).markRedisUnavailable();
        assertThat(recoveryCount("missing_counter", "failure")).isEqualTo(1.0);
        assertThat(fallbackCount("success")).isEqualTo(1.0);
    }

    @Test
    void shouldPropagateExhaustedWithoutFallback() {
        given(modeCoordinator.isRedisReady()).willReturn(true);
        given(counter.tryIncrement(BUSINESS_DATE)).willThrow(new SignNoSequenceExhaustedException());

        assertThatThrownBy(() -> allocationService.next())
                .isInstanceOf(SignNoSequenceExhaustedException.class);

        assertThat(allocationCount("redis", "exhausted")).isEqualTo(1.0);
        verifyNoInteractions(fallbackAllocator);
    }

    @Test
    void shouldPropagateExhaustedFromRecoveryWithoutFallback() {
        given(modeCoordinator.isRedisReady()).willReturn(true);
        given(counter.tryIncrement(BUSINESS_DATE)).willReturn(SignNoIncrementResult.Missing.INSTANCE);
        given(reconciliationService.reconcileAndAllocate(BUSINESS_DATE,
                SignNoRecoveryTrigger.MISSING_COUNTER)).willThrow(new SignNoSequenceExhaustedException());

        assertThatThrownBy(() -> allocationService.next())
                .isInstanceOf(SignNoSequenceExhaustedException.class);

        assertThat(allocationCount("redis", "exhausted")).isEqualTo(1.0);
        verifyNoInteractions(fallbackAllocator);
    }

    @Test
    void shouldRejectRedisSequenceAboveUpperBound() {
        given(modeCoordinator.isRedisReady()).willReturn(true);
        given(counter.tryIncrement(BUSINESS_DATE))
                .willReturn(new SignNoIncrementResult.Allocated(SignNo.MAX_SEQUENCE + 1));

        assertThatThrownBy(() -> allocationService.next())
                .isInstanceOf(SignNoSequenceExhaustedException.class);

        assertThat(allocationCount("redis", "exhausted")).isEqualTo(1.0);
        verifyNoInteractions(fallbackAllocator);
    }

    @Test
    void shouldFallbackWhenRedisReturnsNonPositiveSequence() {
        given(modeCoordinator.isRedisReady()).willReturn(true);
        given(counter.tryIncrement(BUSINESS_DATE)).willReturn(new SignNoIncrementResult.Allocated(0));
        given(signNoPolicy.isFallbackEnabled()).willReturn(true);
        given(fallbackAllocator.nextSequence(BUSINESS_DATE)).willReturn(2L);

        SignNoAllocation allocation = allocationService.next();

        assertThat(allocation.source()).isEqualTo(SignNoSource.MYSQL_FALLBACK);
        verify(modeCoordinator).markRedisUnavailable();
    }

    @Test
    void shouldSkipConflictRecoveryForFallbackSource() {
        allocationService.recoverAfterConflict(BUSINESS_DATE, SignNoSource.MYSQL_FALLBACK);

        verifyNoInteractions(reconciliationService, counter, fallbackAllocator);
    }

    @Test
    void shouldSkipConflictRecoveryWhenInstanceNotReady() {
        allocationService.recoverAfterConflict(BUSINESS_DATE, SignNoSource.REDIS);

        verifyNoInteractions(reconciliationService, counter, fallbackAllocator);
    }

    @Test
    void shouldRecordSuccessfulConflictRecovery() {
        given(modeCoordinator.isRedisReady()).willReturn(true);
        given(reconciliationService.reconcile(BUSINESS_DATE, SignNoRecoveryTrigger.UNIQUE_CONFLICT))
                .willReturn(SignNoReconciliationResult.success(BUSINESS_DATE, 10, 3, 10));

        allocationService.recoverAfterConflict(BUSINESS_DATE, SignNoSource.REDIS);

        assertThat(recoveryCount("unique_conflict", "success")).isEqualTo(1.0);
        verify(modeCoordinator, never()).markRedisUnavailable();
    }

    @Test
    void shouldRecordSkippedConflictRecovery() {
        given(modeCoordinator.isRedisReady()).willReturn(true);
        given(reconciliationService.reconcile(BUSINESS_DATE, SignNoRecoveryTrigger.UNIQUE_CONFLICT))
                .willReturn(SignNoReconciliationResult.skipped(BUSINESS_DATE,
                        SignNoUnavailableReason.RECOVERY_LOCK_TIMEOUT));

        allocationService.recoverAfterConflict(BUSINESS_DATE, SignNoSource.REDIS);

        assertThat(recoveryCount("unique_conflict", "skipped")).isEqualTo(1.0);
        verify(modeCoordinator).markRedisUnavailable();
    }

    @Test
    void shouldKeepReadyWhenConflictRecoveryFailsOnMysqlSide() {
        given(modeCoordinator.isRedisReady()).willReturn(true);
        given(reconciliationService.reconcile(BUSINESS_DATE, SignNoRecoveryTrigger.UNIQUE_CONFLICT))
                .willReturn(SignNoReconciliationResult.failed(BUSINESS_DATE,
                        SignNoUnavailableReason.HIGH_WATER_MARK_UNAVAILABLE));

        allocationService.recoverAfterConflict(BUSINESS_DATE, SignNoSource.REDIS);

        assertThat(recoveryCount("unique_conflict", "failure")).isEqualTo(1.0);
        verify(modeCoordinator, never()).markRedisUnavailable();
    }

    @Test
    void shouldMarkUnavailableWhenConflictRecoveryFailsOnRedisSide() {
        given(modeCoordinator.isRedisReady()).willReturn(true);
        given(reconciliationService.reconcile(BUSINESS_DATE, SignNoRecoveryTrigger.UNIQUE_CONFLICT))
                .willReturn(SignNoReconciliationResult.failed(BUSINESS_DATE,
                        SignNoUnavailableReason.REDIS_UNAVAILABLE));

        allocationService.recoverAfterConflict(BUSINESS_DATE, SignNoSource.REDIS);

        verify(modeCoordinator).markRedisUnavailable();
    }

    private double allocationCount(String source, String result) {
        Counter counter = meterRegistry.find(ALLOCATION_METRIC).tag("source", source).tag("result", result)
                .counter();
        return counter == null ? 0.0 : counter.count();
    }

    private double fallbackCount(String result) {
        Counter counter = meterRegistry.find(FALLBACK_METRIC).tag("result", result).counter();
        return counter == null ? 0.0 : counter.count();
    }

    private double recoveryCount(String trigger, String result) {
        Counter counter = meterRegistry.find(RECOVERY_METRIC).tag("trigger", trigger).tag("result", result)
                .counter();
        return counter == null ? 0.0 : counter.count();
    }
}
