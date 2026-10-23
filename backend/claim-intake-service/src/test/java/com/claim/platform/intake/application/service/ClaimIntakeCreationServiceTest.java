package com.claim.platform.intake.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.claim.platform.intake.application.command.SubmitIntakeCommand;
import com.claim.platform.intake.application.result.ClaimIntakeResult;
import com.claim.platform.intake.domain.model.ClaimIntake;
import com.claim.platform.intake.domain.model.IntakeMaterial;
import com.claim.platform.intake.domain.model.IntakeStatus;
import com.claim.platform.intake.domain.repository.ClaimIntakeStore;
import com.claim.platform.intake.domain.repository.InteractionLogStore;
import com.claim.platform.intake.domain.repository.IntakeMaterialStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

/**
 * 验证标准创建事务对申请、材料和成功日志的原子写入及初始状态规则。
 *
 * @author wangguangwu
 */
@ExtendWith(MockitoExtension.class)
class ClaimIntakeCreationServiceTest {

    private static final String TENANT_CODE = "tenant-a";
    private static final String TRACE_ID = "trace-001";
    private static final String EXTERNAL_APPLY_NO = "EXT001";
    private static final String SIGN_NO = "SIGN001";

    @Mock
    private ClaimIntakeStore intakeStore;
    @Mock
    private IntakeMaterialStore materialStore;
    @Mock
    private InteractionLogStore interactionLogStore;
    @InjectMocks
    private ClaimIntakeCreationService creationService;

    @Test
    void shouldCreateIntakeAsImageNotUploadWhenMaterialsEmpty() {
        ClaimIntakeResult result = creationService.create(command(List.of()), SIGN_NO);

        assertThat(result.signNo()).isEqualTo(SIGN_NO);
        assertThat(result.status()).isEqualTo(IntakeStatus.IMAGE_NOT_UPLOAD);
        ClaimIntake saved = captureSavedIntake();
        assertThat(saved.status()).isEqualTo(IntakeStatus.IMAGE_NOT_UPLOAD);
        verifyNoInteractions(materialStore);
        verify(interactionLogStore).recordSuccess(TENANT_CODE, SIGN_NO, "SIGN_RECEIVE", null, TRACE_ID);
    }

    @Test
    void shouldCreateIntakeAsImageUploadedAndSaveMaterialsWhenMaterialsPresent() {
        List<IntakeMaterial> materials = List.of(
                new IntakeMaterial("BATCH-1", 1, "ID_CARD", "身份证.jpg", "/tmp/a", "/tmp/b"));

        ClaimIntakeResult result = creationService.create(command(materials), SIGN_NO);

        assertThat(result.status()).isEqualTo(IntakeStatus.IMAGE_UPLOADED);
        ClaimIntake saved = captureSavedIntake();
        assertThat(saved.status()).isEqualTo(IntakeStatus.IMAGE_UPLOADED);
        verify(materialStore).saveAll(TENANT_CODE, SIGN_NO, materials);
        verify(interactionLogStore).recordSuccess(TENANT_CODE, SIGN_NO, "SIGN_RECEIVE", null, TRACE_ID);
    }

    private ClaimIntake captureSavedIntake() {
        ArgumentCaptor<ClaimIntake> captor = ArgumentCaptor.forClass(ClaimIntake.class);
        verify(intakeStore).save(captor.capture());
        return captor.getValue();
    }

    private SubmitIntakeCommand command(List<IntakeMaterial> materials) {
        return new SubmitIntakeCommand(TENANT_CODE, TRACE_ID, EXTERNAL_APPLY_NO, "张三", null, null, null,
                null, materials, "{}");
    }
}
