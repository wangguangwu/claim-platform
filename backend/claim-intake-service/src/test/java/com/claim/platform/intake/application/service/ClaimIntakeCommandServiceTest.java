package com.claim.platform.intake.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
import com.claim.platform.intake.application.port.IntakeSubmissionLock;
import com.claim.platform.intake.application.port.IntakeSubmissionLockHandle;
import com.claim.platform.intake.application.port.IntakeSubmissionReplayCache;
import com.claim.platform.intake.application.port.LockAcquireStatus;
import com.claim.platform.intake.application.result.ClaimIntakeResult;
import com.claim.platform.intake.domain.model.ClaimIntake;
import com.claim.platform.intake.domain.model.IntakeMaterial;
import com.claim.platform.intake.domain.model.IntakeStatus;
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

import java.util.List;
import java.util.Optional;

/**
 * 验证标准签收编排的三级幂等、锁失败语义、失败分类与脱敏观测。
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
    private static final String CONFLICT_METRIC = "claim.intake.submission.conflict";

    @Mock
    private ClaimIntakeStore intakeStore;
    @Mock
    private IntakeMaterialStore materialStore;
    @Mock
    private InteractionLogStore interactionLogStore;
    @Mock
    private IntakeSubmissionLock submissionLock;
    @Mock
    private IntakeSubmissionReplayCache replayCache;
    @Mock
    private ClaimIntakeCreationService creationService;
    @Mock
    private SignNoGenerator signNoGenerator;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private ClaimIntakeCommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new ClaimIntakeCommandService(intakeStore, materialStore, interactionLogStore,
                submissionLock, replayCache, creationService, signNoGenerator, meterRegistry);
    }

    @Test
    void shouldReplayCachedSignNoWithCurrentDatabaseStatusWithoutAcquiringLock() {
        given(replayCache.findSignNo(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(Optional.of(SIGN_NO));
        given(intakeStore.findByTenantAndSignNo(TENANT_CODE, SIGN_NO)).willReturn(Optional.of(existingIntake()));

        ClaimIntakeResult result = commandService.submit(command());

        assertThat(result.signNo()).isEqualTo(SIGN_NO);
        assertThat(result.status()).isEqualTo(IntakeStatus.IMAGE_NOT_UPLOAD);
        verify(intakeStore, never()).findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO);
        verifyNoInteractions(submissionLock, creationService, materialStore, interactionLogStore);
        assertThat(submissionCount("replayed_quick")).isEqualTo(1.0);
    }

    @Test
    void shouldFallbackToDatabaseAndWarmCacheWhenReplayCacheMisses() {
        ClaimIntake intake = existingIntake();
        given(replayCache.findSignNo(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(Optional.empty());
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.of(intake));

        ClaimIntakeResult result = commandService.submit(command());

        assertThat(result.signNo()).isEqualTo(SIGN_NO);
        verify(replayCache).put(TENANT_CODE, EXTERNAL_APPLY_NO, SIGN_NO);
        verifyNoInteractions(submissionLock, creationService, materialStore, interactionLogStore);
    }

    @Test
    void shouldEvictStaleCacheMappingBeforeFallingBackToDatabase() {
        given(replayCache.findSignNo(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(Optional.of("STALE"));
        given(intakeStore.findByTenantAndSignNo(TENANT_CODE, "STALE")).willReturn(Optional.empty());
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.of(existingIntake()));

        ClaimIntakeResult result = commandService.submit(command());

        assertThat(result.signNo()).isEqualTo(SIGN_NO);
        verify(replayCache).evict(TENANT_CODE, EXTERNAL_APPLY_NO);
        verify(replayCache).put(TENANT_CODE, EXTERNAL_APPLY_NO, SIGN_NO);
        verifyNoInteractions(submissionLock, creationService, materialStore, interactionLogStore);
    }

    @Test
    void shouldReplayQuickQueryWithoutAcquiringLock() {
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.of(existingIntake()));

        ClaimIntakeResult result = commandService.submit(command());

        assertThat(result.signNo()).isEqualTo(SIGN_NO);
        assertThat(result.status()).isEqualTo(IntakeStatus.IMAGE_NOT_UPLOAD);
        verifyNoInteractions(submissionLock, creationService, materialStore, interactionLogStore);
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
        verify(handle).close();
        assertThat(submissionCount("replayed_locked")).isEqualTo(1.0);
    }

    @Test
    void shouldCreateIntakeWhenBothQueriesMiss() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        ClaimIntakeResult created = new ClaimIntakeResult("SIGNNEW", IntakeStatus.IMAGE_UPLOADED);
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty());
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);
        given(signNoGenerator.generate()).willReturn("SIGNNEW");
        given(creationService.create(any(SubmitIntakeCommand.class), eq("SIGNNEW"))).willReturn(created);

        ClaimIntakeResult result = commandService.submit(command());

        assertThat(result).isSameAs(created);
        verify(replayCache).put(TENANT_CODE, EXTERNAL_APPLY_NO, "SIGNNEW");
        verify(handle).close();
        assertThat(submissionCount("created")).isEqualTo(1.0);
    }

    @Test
    void shouldReplayConcurrentWinnerAfterIdempotencyKeyConflict() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        DuplicateKeyException conflict = new DuplicateKeyException("duplicate external apply no");
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty(), Optional.empty(), Optional.of(existingIntake()));
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);
        given(signNoGenerator.generate()).willReturn("SIGNNEW");
        given(creationService.create(any(SubmitIntakeCommand.class), eq("SIGNNEW"))).willThrow(conflict);

        ClaimIntakeResult result = commandService.submit(command());

        assertThat(result.signNo()).isEqualTo(SIGN_NO);
        verify(handle).close();
        assertThat(conflictCount("external_apply_no")).isEqualTo(1.0);
        assertThat(submissionCount("replayed_conflict")).isEqualTo(1.0);
    }

    @Test
    void shouldRegenerateSignNoOnceWhenConflictIsNotIdempotencyKey() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        ClaimIntakeResult retried = new ClaimIntakeResult("SIGN0002", IntakeStatus.IMAGE_NOT_UPLOAD);
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty());
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);
        given(signNoGenerator.generate()).willReturn("SIGN0001", "SIGN0002");
        given(creationService.create(any(SubmitIntakeCommand.class), eq("SIGN0001")))
                .willThrow(new DuplicateKeyException("duplicate sign no"));
        given(creationService.create(any(SubmitIntakeCommand.class), eq("SIGN0002"))).willReturn(retried);

        ClaimIntakeResult result = commandService.submit(command());

        assertThat(result).isSameAs(retried);
        verify(creationService).create(any(SubmitIntakeCommand.class), eq("SIGN0001"));
        verify(creationService).create(any(SubmitIntakeCommand.class), eq("SIGN0002"));
        assertThat(conflictCount("sign_no_retry")).isEqualTo(1.0);
    }

    @Test
    void shouldFailAfterSecondSignNoCollision() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        DuplicateKeyException conflict = new DuplicateKeyException("duplicate sign no");
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty());
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);
        given(signNoGenerator.generate()).willReturn("SIGN0001", "SIGN0002");
        given(creationService.create(any(SubmitIntakeCommand.class), anyString())).willThrow(conflict);

        assertThatThrownBy(() -> commandService.submit(command())).isSameAs(conflict);

        verify(handle).close();
        assertThat(conflictCount("sign_no_retry")).isEqualTo(1.0);
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
        verifyNoInteractions(creationService, materialStore, interactionLogStore);
        assertThat(submissionCount("lock_timeout")).isEqualTo(1.0);
        assertThat(submissionCount("error")).isZero();
    }

    @Test
    void shouldDegradeWhenRedisUnavailable() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.UNAVAILABLE);
        ClaimIntakeResult created = new ClaimIntakeResult("SIGNNEW", IntakeStatus.IMAGE_NOT_UPLOAD);
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty());
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);
        given(signNoGenerator.generate()).willReturn("SIGNNEW");
        given(creationService.create(any(SubmitIntakeCommand.class), eq("SIGNNEW"))).willReturn(created);

        ClaimIntakeResult result = commandService.submit(command());

        assertThat(result).isSameAs(created);
        verify(handle).close();
        assertThat(submissionCount("lock_degraded")).isEqualTo(1.0);
        assertThat(submissionCount("created")).isEqualTo(1.0);
    }

    @Test
    void shouldReleaseLockWhenCreationFails() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        IllegalStateException failure = new IllegalStateException("creation failed");
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty());
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);
        given(signNoGenerator.generate()).willReturn("SIGNNEW");
        given(creationService.create(any(SubmitIntakeCommand.class), eq("SIGNNEW"))).willThrow(failure);

        assertThatThrownBy(() -> commandService.submit(command())).isSameAs(failure);

        verify(replayCache, never()).put(TENANT_CODE, EXTERNAL_APPLY_NO, "SIGNNEW");
        verify(handle).close();
        assertThat(submissionCount("error")).isEqualTo(1.0);
    }

    @Test
    void shouldPropagateNonUniqueIntegrityFailureWithoutReplay() {
        IntakeSubmissionLockHandle handle = handle(LockAcquireStatus.ACQUIRED);
        DataIntegrityViolationException failure = new DataIntegrityViolationException("not a unique key");
        given(intakeStore.findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO))
                .willReturn(Optional.empty());
        given(submissionLock.tryAcquire(TENANT_CODE, EXTERNAL_APPLY_NO)).willReturn(handle);
        given(signNoGenerator.generate()).willReturn("SIGNNEW");
        given(creationService.create(any(SubmitIntakeCommand.class), eq("SIGNNEW"))).willThrow(failure);

        assertThatThrownBy(() -> commandService.submit(command())).isSameAs(failure);

        verify(intakeStore, times(2)).findByTenantAndExternalApplyNo(TENANT_CODE, EXTERNAL_APPLY_NO);
        verify(handle).close();
        assertThat(conflictCount("external_apply_no")).isZero();
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
}
