package com.claim.platform.intake.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.claim.platform.intake.application.command.SubmitIntakeCommand;
import com.claim.platform.intake.application.result.ClaimIntakeResult;
import com.claim.platform.intake.domain.model.ClaimIntake;
import com.claim.platform.intake.domain.repository.ClaimIntakeStore;
import com.claim.platform.intake.domain.repository.InteractionLogStore;
import com.claim.platform.intake.domain.repository.IntakeMaterialStore;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

/**
 * 验证受理命令使用原始受理表唯一键实现的天然幂等行为。
 *
 * @author wangguangwu
 */
@ExtendWith(MockitoExtension.class)
class ClaimIntakeCommandServiceTest {

    @Mock
    private ClaimIntakeStore intakeStore;
    @Mock
    private IntakeMaterialStore materialStore;
    @Mock
    private InteractionLogStore interactionLogStore;
    @InjectMocks
    private ClaimIntakeCommandService commandService;

    @Test
    void shouldReplayExistingIntakeForSameTenantAndExternalApplyNo() {
        ClaimIntake existing = ClaimIntake.create("tenant-a", "SIGN001", "EXT001", "张三", null,
                null, null, null, null);
        given(intakeStore.findByTenantAndExternalApplyNo("tenant-a", "EXT001"))
                .willReturn(Optional.of(existing));

        ClaimIntakeResult result = commandService.submit(command());

        assertThat(result.signNo()).isEqualTo("SIGN001");
    }

    @Test
    void shouldReplayExistingIntakeWhenUniqueConstraintWinsConcurrentRace() {
        ClaimIntake existing = ClaimIntake.create("tenant-a", "SIGN002", "EXT001", "张三", null,
                null, null, null, null);
        AtomicInteger lookupCount = new AtomicInteger();
        given(intakeStore.findByTenantAndExternalApplyNo("tenant-a", "EXT001"))
                .willAnswer(invocation -> lookupCount.getAndIncrement() == 0
                        ? Optional.empty() : Optional.of(existing));
        org.mockito.BDDMockito.willThrow(new DuplicateKeyException("duplicate"))
                .given(intakeStore).save(any(ClaimIntake.class));

        ClaimIntakeResult result = commandService.submit(command());

        assertThat(result.signNo()).isEqualTo("SIGN002");
    }

    private SubmitIntakeCommand command() {
        return new SubmitIntakeCommand("tenant-a", "trace-001", "EXT001", "张三", null,
                null, null, null, null);
    }
}
