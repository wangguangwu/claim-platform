package com.claim.platform.sign.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.claim.platform.sign.application.exception.SignNoUnavailableReason;
import com.claim.platform.sign.application.result.ReconciliationStatus;
import com.claim.platform.sign.application.result.SignNoReconciliationResult;
import com.claim.platform.sign.application.result.SignNoRecoveryTrigger;
import com.claim.platform.sign.application.service.SignNoMetrics;
import com.claim.platform.sign.application.service.SignNoModeCoordinator;
import com.claim.platform.sign.application.service.SignNoReconciliationService;
import com.claim.platform.sign.infrastructure.config.SignNoProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 验证后台校准的启动/周期触发、日期范围、就绪切换、失败隔离、重叠跳过和指标。
 *
 * @author wangguangwu
 */
@ExtendWith(MockitoExtension.class)
class SignNoReconciliationSchedulerTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 10, 23);
    private static final String SEQUENCE_GAP_METRIC = "claim.intake.number.sequence_gap";
    private static final String RECOVERY_METRIC = "claim.intake.number.recovery";

    @Mock
    private SignNoReconciliationService reconciliationService;
    @Mock
    private SignNoModeCoordinator modeCoordinator;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final SignNoMetrics metrics = new SignNoMetrics(meterRegistry);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    private SignNoProperties properties;
    private SignNoReconciliationScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new SignNoProperties();
        properties.setReconciliationInitialDelay(Duration.ofHours(1));
        properties.setReconciliationInterval(Duration.ofHours(1));
        scheduler = new SignNoReconciliationScheduler(reconciliationService, modeCoordinator, metrics,
                properties, clock);
    }

    @AfterEach
    void tearDown() {
        scheduler.stop();
    }

    @Test
    void shouldReconcileManualCurrentDateAndMarkReady() throws Exception {
        given(reconciliationService.reconcile(BUSINESS_DATE, SignNoRecoveryTrigger.MANUAL))
                .willReturn(SignNoReconciliationResult.success(BUSINESS_DATE, 5, 3, 5));
        scheduler.start();

        SignNoReconciliationResult result =
                scheduler.requestAsync(BUSINESS_DATE).get(2, TimeUnit.SECONDS);

        assertThat(result.status()).isEqualTo(ReconciliationStatus.SUCCESS);
        verify(modeCoordinator).markRedisReady();
        assertThat(recoveryCount("manual", "success")).isEqualTo(1.0);
        assertThat(sequenceGap("current")).isEqualTo(2.0);
    }

    @Test
    void shouldNotMarkReadyWhenReconcilingPreviousDate() throws Exception {
        LocalDate previousDate = BUSINESS_DATE.minusDays(1);
        given(reconciliationService.reconcile(previousDate, SignNoRecoveryTrigger.MANUAL))
                .willReturn(SignNoReconciliationResult.success(previousDate, 8, 5, 8));
        scheduler.start();

        SignNoReconciliationResult result =
                scheduler.requestAsync(previousDate).get(2, TimeUnit.SECONDS);

        assertThat(result.status()).isEqualTo(ReconciliationStatus.SUCCESS);
        verifyNoInteractions(modeCoordinator);
        assertThat(sequenceGap("previous")).isEqualTo(3.0);
    }

    @Test
    void shouldMarkUnavailableWhenManualReconcileFailsOnRedisSide() throws Exception {
        given(reconciliationService.reconcile(BUSINESS_DATE, SignNoRecoveryTrigger.MANUAL))
                .willReturn(SignNoReconciliationResult.failed(BUSINESS_DATE,
                        SignNoUnavailableReason.REDIS_UNAVAILABLE));
        scheduler.start();

        SignNoReconciliationResult result =
                scheduler.requestAsync(BUSINESS_DATE).get(2, TimeUnit.SECONDS);

        assertThat(result.status()).isEqualTo(ReconciliationStatus.FAILED);
        verify(modeCoordinator).markRedisUnavailable();
        verify(modeCoordinator, never()).markRedisReady();
        assertThat(recoveryCount("manual", "failure")).isEqualTo(1.0);
        assertThat(meterRegistry.find(SEQUENCE_GAP_METRIC).gauge()).isNull();
    }

    @Test
    void shouldKeepReadinessWhenManualReconcileFailsOnMysqlSide() throws Exception {
        given(reconciliationService.reconcile(BUSINESS_DATE, SignNoRecoveryTrigger.MANUAL))
                .willReturn(SignNoReconciliationResult.failed(BUSINESS_DATE,
                        SignNoUnavailableReason.HIGH_WATER_MARK_UNAVAILABLE));
        scheduler.start();

        scheduler.requestAsync(BUSINESS_DATE).get(2, TimeUnit.SECONDS);

        verifyNoInteractions(modeCoordinator);
        assertThat(recoveryCount("manual", "failure")).isEqualTo(1.0);
    }

    @Test
    void shouldSkipManualRequestWhileRoundIsRunning() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        given(reconciliationService.reconcile(BUSINESS_DATE, SignNoRecoveryTrigger.MANUAL))
                .willAnswer(invocation -> {
                    entered.countDown();
                    release.await(2, TimeUnit.SECONDS);
                    return SignNoReconciliationResult.success(BUSINESS_DATE, 5, 3, 5);
                });
        scheduler.start();
        CompletableFuture<SignNoReconciliationResult> first = scheduler.requestAsync(BUSINESS_DATE);
        try {
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();

            SignNoReconciliationResult skipped =
                    scheduler.requestAsync(BUSINESS_DATE).get(1, TimeUnit.SECONDS);

            assertThat(skipped.status()).isEqualTo(ReconciliationStatus.SKIPPED);
            assertThat(skipped.failureReason()).isNull();
            assertThat(recoveryCount("manual", "skipped")).isEqualTo(1.0);
        } finally {
            release.countDown();
        }

        assertThat(first.get(2, TimeUnit.SECONDS).status()).isEqualTo(ReconciliationStatus.SUCCESS);
    }

    @Test
    void shouldRunStartupRoundForCurrentAndPreviousDate() {
        properties.setReconciliationInitialDelay(Duration.ZERO);
        properties.setReconciliationDays(2);
        given(reconciliationService.reconcile(any(LocalDate.class), any(SignNoRecoveryTrigger.class)))
                .willAnswer(invocation -> SignNoReconciliationResult.success(
                        invocation.getArgument(0), 5, 3, 5));
        scheduler.start();

        verify(reconciliationService, timeout(2000).times(2))
                .reconcile(any(LocalDate.class), eq(SignNoRecoveryTrigger.STARTUP));
        verify(modeCoordinator, timeout(2000)).markRedisReady();

        ArgumentCaptor<LocalDate> dates = ArgumentCaptor.forClass(LocalDate.class);
        verify(reconciliationService, timeout(2000).times(2))
                .reconcile(dates.capture(), eq(SignNoRecoveryTrigger.STARTUP));
        assertThat(dates.getAllValues())
                .containsExactlyInAnyOrder(BUSINESS_DATE, BUSINESS_DATE.minusDays(1));
        assertThat(sequenceGap("current")).isEqualTo(2.0);
        assertThat(sequenceGap("previous")).isEqualTo(2.0);
    }

    @Test
    void shouldIsolateFailurePerDateAndKeepProcessing() {
        properties.setReconciliationInitialDelay(Duration.ZERO);
        properties.setReconciliationDays(2);
        given(reconciliationService.reconcile(BUSINESS_DATE, SignNoRecoveryTrigger.STARTUP))
                .willThrow(new IllegalStateException("unexpected failure"));
        given(reconciliationService.reconcile(BUSINESS_DATE.minusDays(1), SignNoRecoveryTrigger.STARTUP))
                .willReturn(SignNoReconciliationResult.success(BUSINESS_DATE.minusDays(1), 5, 5, 5));
        scheduler.start();

        verify(reconciliationService, timeout(2000))
                .reconcile(BUSINESS_DATE.minusDays(1), SignNoRecoveryTrigger.STARTUP);
        verify(modeCoordinator, after(200).never()).markRedisReady();
        assertThat(recoveryCount("startup", "failure")).isEqualTo(1.0);
        assertThat(recoveryCount("startup", "success")).isEqualTo(1.0);
    }

    @Test
    void shouldUsePeriodicTriggerAfterStartupRound() {
        properties.setReconciliationInitialDelay(Duration.ZERO);
        properties.setReconciliationInterval(Duration.ofMillis(100));
        properties.setReconciliationDays(1);
        given(reconciliationService.reconcile(any(LocalDate.class), any(SignNoRecoveryTrigger.class)))
                .willReturn(SignNoReconciliationResult.success(BUSINESS_DATE, 5, 5, 5));
        scheduler.start();

        ArgumentCaptor<SignNoRecoveryTrigger> triggers = ArgumentCaptor.forClass(SignNoRecoveryTrigger.class);
        verify(reconciliationService, timeout(2000).atLeast(2))
                .reconcile(eq(BUSINESS_DATE), triggers.capture());

        assertThat(triggers.getAllValues()).startsWith(SignNoRecoveryTrigger.STARTUP,
                SignNoRecoveryTrigger.PERIODIC);
    }

    private double sequenceGap(String scope) {
        Gauge gauge = meterRegistry.find(SEQUENCE_GAP_METRIC).tag("scope", scope).gauge();
        assertThat(gauge).isNotNull();
        return gauge.value();
    }

    private double recoveryCount(String trigger, String result) {
        Counter counter = meterRegistry.find(RECOVERY_METRIC).tag("trigger", trigger).tag("result", result)
                .counter();
        return counter == null ? 0.0 : counter.count();
    }
}
