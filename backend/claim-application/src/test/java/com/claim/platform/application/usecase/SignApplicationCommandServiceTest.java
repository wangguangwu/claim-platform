package com.claim.platform.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.claim.platform.application.domain.model.SignApplication;
import com.claim.platform.application.domain.repository.InteractionLogStore;
import com.claim.platform.application.domain.repository.SignApplicationStore;
import com.claim.platform.application.domain.repository.SignMaterialStore;
import com.claim.platform.application.usecase.command.SubmitSignApplicationCommand;
import com.claim.platform.application.usecase.result.SignApplicationResult;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

/**
 * 验证签收命令使用原始申请表唯一键实现的天然幂等行为。
 *
 * @author wangguangwu
 */
@ExtendWith(MockitoExtension.class)
class SignApplicationCommandServiceTest {

    @Mock
    private SignApplicationStore applicationStore;
    @Mock
    private SignMaterialStore materialStore;
    @Mock
    private InteractionLogStore interactionLogStore;
    @InjectMocks
    private SignApplicationCommandService commandService;

    @Test
    void shouldReplayExistingApplicationForSameTenantAndExternalApplyNo() {
        SignApplication existing = SignApplication.create("tenant-a", "SIGN001", "EXT001", "张三", null,
                null, null, null, null);
        given(applicationStore.findByTenantAndExternalApplyNo("tenant-a", "EXT001"))
                .willReturn(Optional.of(existing));

        SignApplicationResult result = commandService.submit(command());

        assertThat(result.signNo()).isEqualTo("SIGN001");
    }

    @Test
    void shouldReplayExistingApplicationWhenUniqueConstraintWinsConcurrentRace() {
        SignApplication existing = SignApplication.create("tenant-a", "SIGN002", "EXT001", "张三", null,
                null, null, null, null);
        AtomicInteger lookupCount = new AtomicInteger();
        given(applicationStore.findByTenantAndExternalApplyNo("tenant-a", "EXT001"))
                .willAnswer(invocation -> lookupCount.getAndIncrement() == 0
                        ? Optional.empty() : Optional.of(existing));
        org.mockito.BDDMockito.willThrow(new DuplicateKeyException("duplicate"))
                .given(applicationStore).save(any(SignApplication.class));

        SignApplicationResult result = commandService.submit(command());

        assertThat(result.signNo()).isEqualTo("SIGN002");
    }

    private SubmitSignApplicationCommand command() {
        return new SubmitSignApplicationCommand("tenant-a", "trace-001", "EXT001", "张三", null,
                null, null, null, null);
    }
}
