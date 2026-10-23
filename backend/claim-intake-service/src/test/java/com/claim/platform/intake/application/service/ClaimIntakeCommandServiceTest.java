package com.claim.platform.intake.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.claim.platform.intake.application.command.SubmitIntakeCommand;
import com.claim.platform.intake.application.exception.IntakeSubmissionInProgressException;
import com.claim.platform.intake.application.exception.SignNoUnavailableException;
import com.claim.platform.intake.application.exception.SignNoUnavailableReason;
import com.claim.platform.intake.application.port.IntakeSubmissionLock;
import com.claim.platform.intake.application.port.IntakeSubmissionLockHandle;
import com.claim.platform.intake.application.port.LockAcquireStatus;
import com.claim.platform.intake.application.port.SignNoPolicy;
import com.claim.platform.intake.application.result.ClaimIntakeResult;
import com.claim.platform.intake.application.result.SignNoAllocation;
import com.claim.platform.intake.application.result.SignNoSource;
import com.claim.platform.intake.domain.model.ClaimIntake;
import com.claim.platform.intake.domain.model.IntakeMaterial;
import com.claim.platform.intake.domain.model.IntakeStatus;
import com.claim.platform.intake.domain.model.SignNoFormat;
import com.claim.platform.intake.domain.model.person.Applicant;
import com.claim.platform.intake.domain.repository.ClaimIntakeStore;
import com.claim.platform.intake.domain.repository.InteractionLogStore;
import com.claim.platform.intake.domain.repository.IntakeMaterialStore;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 验证标准签收编排的三级幂等、签收号冲突重试、失败分类与脱敏观测。
 *
 * @author wangguangwu
 */
@ExtendWith(MockitoExtension.class)
class ClaimIntakeCommandServiceTest {

    private static final String TENANT_CODE = "tenant-a";
    private static final String EXTERNAL_APPLY_NO = "EXT001";
    private static final String TRACE_ID = "trace-001";
    private static final String SIGN_NO = "SIGN001";
    private static final String SUBMISSION_METRIC = "claim.intake.submission";
    private static final String CONFLICT_METRIC = "claim.intake.number.conflict";
    private static final String FALLBACK_METRIC = "claim.intake.number.fallback";
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 10, 23);

    @Mock
    private ClaimIntakeStore intakeStore;
    @Mock
    private IntakeMaterialStore materialStore;
    @Mock
    private InteractionLogStore interactionLogStore;
    @Mock
    private IntakeSubmissionLock submissionLock;
    @Mock
    private ClaimIntakeCreationService creationService;
    @Mock
    private SignNoAllocationService signNoAllocationService;
    @Mock
    private SignNoPolicy signNoPolicy;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private ClaimIntakeCommandService commandService;

    @BeforeEach
    void setUp() {
        lenient().when(signNoPolicy.getMaxAttempts()).thenReturn(3);
        commandService = new ClaimIntakeCommandService(intakeStore, materialStore, interactionLogStore,
                submissionLock, creationService, signNoAllocationService, signNoPolicy,
                new SignNoMetrics(meterRegistry), meterRegistry);
    }

    @Test
    void shouldReplayQuickQueryWithoutAcquiringLock() {
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.of(existingIntake()));

        ClaimIntakeResult result = commandService.submit(command());

        assertThat(result.signNo()).isEqualTo(SIGN_NO);
        assertThat(result.status()).isEqualTo(IntakeStatus.IMAGE_NOT_UPLOAD);
        verifyNoInteractions(submissionLock, creationService, signNoAllocationService, materialStore,
                interactionLogStore);
        assertThat(submissionCount("replayed_quick")).isEqualTo(1.0);
    }

    @Test
    void shouldReplayLockedDoubleCheckWithoutCreating() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty(), Optional.of(existingIntake()));
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);

        ClaimIntakeResult result = commandService.submit(command());

        assertThat(result.signNo()).isEqualTo(SIGN_NO);
        verify(creationService, never()).create(any(), anyString());
        verifyNoInteractions(signNoAllocationService);
        verify(handle).close();
        assertThat(submissionCount("replayed_locked")).isEqualTo(1.0);
    }

    @Test
    void shouldCreateIntakeWithAllocatedSignNo() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        String signNo = SignNoFormat.format(BUSINESS_DATE, 1);
        ClaimIntakeResult created = new ClaimIntakeResult(signNo, IntakeStatus.IMAGE_UPLOADED);
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty());
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);
        given(signNoAllocationService.next()).willReturn(new SignNoAllocation(signNo, SignNoSource.REDIS));
        given(creationService.create(any(SubmitIntakeCommand.class), eq(signNo))).willReturn(created);

        ClaimIntakeResult result = commandService.submit(command());

        assertThat(result).isSameAs(created);
        verify(handle).close();
        assertThat(submissionCount("created")).isEqualTo(1.0);
    }

    @Test
    void shouldReplayConcurrentWinnerAfterIdempotencyKeyConflict() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        String signNo = SignNoFormat.format(BUSINESS_DATE, 1);
        DuplicateKeyException conflict = new DuplicateKeyException("duplicate external apply no");
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty(), Optional.empty(), Optional.of(existingIntake()));
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);
        given(signNoPolicy.getMaxAttempts()).willReturn(3);
        given(signNoAllocationService.next()).willReturn(new SignNoAllocation(signNo, SignNoSource.REDIS));
        given(creationService.create(any(SubmitIntakeCommand.class), eq(signNo))).willThrow(conflict);

        ClaimIntakeResult result = commandService.submit(command());

        assertThat(result.signNo()).isEqualTo(SIGN_NO);
        verify(handle).close();
        verify(signNoAllocationService, never()).recoverAfterConflict(any(), any());
        assertThat(conflictCount("external_apply_no")).isEqualTo(1.0);
        assertThat(submissionCount("replayed_conflict")).isEqualTo(1.0);
    }

    @Test
    void shouldRecoverAndRetryWhenSignNoCollides() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        String firstSignNo = SignNoFormat.format(BUSINESS_DATE, 1);
        String secondSignNo = SignNoFormat.format(BUSINESS_DATE, 2);
        ClaimIntakeResult retried = new ClaimIntakeResult(secondSignNo, IntakeStatus.IMAGE_NOT_UPLOAD);
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty());
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);
        given(signNoPolicy.getMaxAttempts()).willReturn(2);
        given(signNoAllocationService.next()).willReturn(
                new SignNoAllocation(firstSignNo, SignNoSource.REDIS),
                new SignNoAllocation(secondSignNo, SignNoSource.REDIS));
        given(creationService.create(any(SubmitIntakeCommand.class), eq(firstSignNo)))
                .willThrow(new DuplicateKeyException("duplicate sign no"));
        given(creationService.create(any(SubmitIntakeCommand.class), eq(secondSignNo))).willReturn(retried);

        ClaimIntakeResult result = commandService.submit(command());

        assertThat(result).isSameAs(retried);
        verify(signNoAllocationService).recoverAfterConflict(BUSINESS_DATE, SignNoSource.REDIS);
        assertThat(conflictCount("sign_no")).isEqualTo(1.0);
        assertThat(submissionCount("created")).isEqualTo(1.0);
    }

    @Test
    void shouldFailAfterConfiguredAttemptsWithoutRecoveringOnLastConflict() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        String firstSignNo = SignNoFormat.format(BUSINESS_DATE, 1);
        String secondSignNo = SignNoFormat.format(BUSINESS_DATE, 2);
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty());
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);
        given(signNoPolicy.getMaxAttempts()).willReturn(2);
        given(signNoAllocationService.next()).willReturn(
                new SignNoAllocation(firstSignNo, SignNoSource.REDIS),
                new SignNoAllocation(secondSignNo, SignNoSource.REDIS));
        given(creationService.create(any(SubmitIntakeCommand.class), anyString()))
                .willThrow(new DuplicateKeyException("duplicate sign no"));

        assertThatThrownBy(() -> commandService.submit(command()))
                .isInstanceOf(SignNoUnavailableException.class)
                .satisfies(exception -> assertThat(((SignNoUnavailableException) exception).reason())
                        .isEqualTo(SignNoUnavailableReason.RETRY_EXHAUSTED));

        verify(handle).close();
        verify(signNoAllocationService, times(1)).recoverAfterConflict(BUSINESS_DATE, SignNoSource.REDIS);
        verify(signNoAllocationService, times(2)).next();
        assertThat(conflictCount("sign_no")).isEqualTo(2.0);
        assertThat(submissionCount("error")).isEqualTo(1.0);
    }

    @Test
    void shouldDelegateFallbackConflictRecoveryWithoutRedisCalibration() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        String firstSignNo = SignNoFormat.format(BUSINESS_DATE, 1);
        String secondSignNo = SignNoFormat.format(BUSINESS_DATE, 2);
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty());
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);
        given(signNoPolicy.getMaxAttempts()).willReturn(2);
        given(signNoAllocationService.next()).willReturn(
                new SignNoAllocation(firstSignNo, SignNoSource.MYSQL_FALLBACK),
                new SignNoAllocation(secondSignNo, SignNoSource.MYSQL_FALLBACK));
        given(creationService.create(any(SubmitIntakeCommand.class), anyString()))
                .willThrow(new DuplicateKeyException("duplicate sign no"));

        assertThatThrownBy(() -> commandService.submit(command()))
                .isInstanceOf(SignNoUnavailableException.class)
                .satisfies(exception -> assertThat(((SignNoUnavailableException) exception).reason())
                        .isEqualTo(SignNoUnavailableReason.RETRY_EXHAUSTED));

        verify(signNoAllocationService).recoverAfterConflict(BUSINESS_DATE, SignNoSource.MYSQL_FALLBACK);
        assertThat(conflictCount("sign_no")).isEqualTo(2.0);
        assertThat(fallbackCount("conflict")).isEqualTo(2.0);
    }

    @Test
    void shouldPropagateSignNoUnavailableWithoutCreating() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        SignNoUnavailableException failure =
                new SignNoUnavailableException(SignNoUnavailableReason.FALLBACK_DISABLED);
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty());
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);
        given(signNoAllocationService.next()).willThrow(failure);

        assertThatThrownBy(() -> commandService.submit(command())).isSameAs(failure);

        verify(creationService, never()).create(any(), anyString());
        verify(handle).close();
        assertThat(submissionCount("error")).isEqualTo(1.0);
    }

    @Test
    void shouldThrowSubmissionInProgressWhenLockTimeout() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.TIMEOUT);
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty());
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);

        assertThatThrownBy(() -> commandService.submit(command()))
                .isInstanceOf(IntakeSubmissionInProgressException.class)
                .hasMessage("申请正在处理中，请稍后重试");

        verify(handle).close();
        verifyNoInteractions(creationService, signNoAllocationService, materialStore, interactionLogStore);
        assertThat(submissionCount("lock_timeout")).isEqualTo(1.0);
        assertThat(submissionCount("error")).isZero();
    }

    @Test
    void shouldDegradeWhenSubmissionLockUnavailable() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.UNAVAILABLE);
        String signNo = SignNoFormat.format(BUSINESS_DATE, 1);
        ClaimIntakeResult created = new ClaimIntakeResult(signNo, IntakeStatus.IMAGE_NOT_UPLOAD);
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty());
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);
        given(signNoAllocationService.next()).willReturn(
                new SignNoAllocation(signNo, SignNoSource.MYSQL_FALLBACK));
        given(creationService.create(any(SubmitIntakeCommand.class), eq(signNo))).willReturn(created);

        ClaimIntakeResult result = commandService.submit(command());

        assertThat(result).isSameAs(created);
        verify(handle).close();
        assertThat(submissionCount("lock_degraded")).isEqualTo(1.0);
        assertThat(submissionCount("created")).isEqualTo(1.0);
    }

    @Test
    void shouldReleaseLockWhenCreationFails() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        String signNo = SignNoFormat.format(BUSINESS_DATE, 1);
        IllegalStateException failure = new IllegalStateException("creation failed");
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty());
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);
        given(signNoAllocationService.next()).willReturn(new SignNoAllocation(signNo, SignNoSource.REDIS));
        given(creationService.create(any(SubmitIntakeCommand.class), eq(signNo))).willThrow(failure);

        assertThatThrownBy(() -> commandService.submit(command())).isSameAs(failure);

        verify(handle).close();
        assertThat(submissionCount("error")).isEqualTo(1.0);
    }

    @Test
    void shouldPropagateNonUniqueIntegrityFailureWithoutRetry() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        String signNo = SignNoFormat.format(BUSINESS_DATE, 1);
        DataIntegrityViolationException failure = new DataIntegrityViolationException("not a unique key");
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty());
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);
        given(signNoAllocationService.next()).willReturn(new SignNoAllocation(signNo, SignNoSource.REDIS));
        given(creationService.create(any(SubmitIntakeCommand.class), eq(signNo))).willThrow(failure);

        assertThatThrownBy(() -> commandService.submit(command())).isSameAs(failure);

        verify(intakeStore, times(2)).findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO);
        verify(signNoAllocationService, times(1)).next();
        verify(handle).close();
        assertThat(conflictCount("external_apply_no")).isZero();
        assertThat(conflictCount("sign_no")).isZero();
    }

    @Test
    void shouldNotLogSensitiveCommandValues() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.TIMEOUT);
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty());
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);
        Logger logger = (Logger) LoggerFactory.getLogger(ClaimIntakeCommandService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThatThrownBy(() -> commandService.submit(command()))
                    .isInstanceOf(IntakeSubmissionInProgressException.class);
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).isNotEmpty();
        assertThat(appender.list.stream().map(ILoggingEvent::getFormattedMessage))
                .noneMatch(message -> message.contains("110101199001011234")
                        || message.contains("13800138000")
                        || message.contains("BATCH-1")
                        || message.contains("/tmp/a"));
        assertThat(meterRegistry.get(SUBMISSION_METRIC).counters())
                .allSatisfy(counter -> assertThat(counter.getId().getTags())
                        .allSatisfy(tag -> assertThat(tag.getKey()).isEqualTo("result")));
    }

    private ClaimIntake existingIntake() {
        return ClaimIntake.create(TENANT_CODE, SIGN_NO, EXTERNAL_APPLY_NO, "张三", null, null, null, null, null);
    }

    private SubmitIntakeCommand command() {
        Applicant applicant = new Applicant("张三", "ID_CARD", "110101199001011234", "13800138000", "M",
                "1990-01-01", "SELF");
        IntakeMaterial material = new IntakeMaterial("BATCH-1", 1, "ID_CARD", "身份证.jpg", "/tmp/a", "/tmp/b");
        return new SubmitIntakeCommand(TENANT_CODE, TRACE_ID, EXTERNAL_APPLY_NO, "张三", applicant, null, null,
                null, List.of(material), "{}");
    }

    private IntakeSubmissionLockHandle handle(LockAcquireStatus status) {
        IntakeSubmissionLockHandle handle = mock(IntakeSubmissionLockHandle.class);
        given(handle.status()).willReturn(status);
        return handle;
    }

    private double submissionCount(String result) {
        Counter counter = meterRegistry.find(SUBMISSION_METRIC).tag("result", result).counter();
        return counter == null ? 0.0 : counter.count();
    }

    private double conflictCount(String type) {
        Counter counter = meterRegistry.find(CONFLICT_METRIC).tag("type", type).counter();
        return counter == null ? 0.0 : counter.count();
    }

    private double fallbackCount(String result) {
        Counter counter = meterRegistry.find(FALLBACK_METRIC).tag("result", result).counter();
        return counter == null ? 0.0 : counter.count();
    }
}
