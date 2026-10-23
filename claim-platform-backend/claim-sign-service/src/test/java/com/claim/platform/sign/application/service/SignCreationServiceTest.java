package com.claim.platform.sign.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.claim.platform.sign.application.command.SignCommand;
import com.claim.platform.sign.application.result.SignResult;
import com.claim.platform.sign.domain.model.Sign;
import com.claim.platform.sign.domain.model.Material;
import com.claim.platform.sign.domain.model.SignStatus;
import com.claim.platform.sign.domain.repository.SignRepository;
import com.claim.platform.sign.domain.repository.InteractionLogRepository;
import com.claim.platform.sign.domain.repository.MaterialRepository;
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
class SignCreationServiceTest {

    private static final String TENANT_CODE = "tenant-a";
    private static final String TRACE_ID = "trace-001";
    private static final String EXTERNAL_APPLY_NO = "EXT001";
    private static final String SIGN_NO = "SIGN001";

    @Mock
    private SignRepository signRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private InteractionLogRepository interactionLogRepository;
    @InjectMocks
    private SignCreationService creationService;

    @Test
    void shouldCreateSignAsImageNotUploadWhenMaterialsEmpty() {
        SignResult result = creationService.create(command(List.of()), SIGN_NO);

        assertThat(result.signNo()).isEqualTo(SIGN_NO);
        assertThat(result.status()).isEqualTo(SignStatus.IMAGE_NOT_UPLOAD);
        Sign saved = captureSavedSign();
        assertThat(saved.status()).isEqualTo(SignStatus.IMAGE_NOT_UPLOAD);
        verifyNoInteractions(materialRepository);
        verify(interactionLogRepository).recordSuccess(TENANT_CODE, SIGN_NO, "SIGN_RECEIVE", null, TRACE_ID);
    }

    @Test
    void shouldCreateSignAsImageUploadedAndSaveMaterialsWhenMaterialsPresent() {
        List<Material> materials = List.of(
                new Material("BATCH-1", 1, "ID_CARD", "身份证.jpg", "/tmp/a", "/tmp/b"));

        SignResult result = creationService.create(command(materials), SIGN_NO);

        assertThat(result.status()).isEqualTo(SignStatus.IMAGE_UPLOADED);
        Sign saved = captureSavedSign();
        assertThat(saved.status()).isEqualTo(SignStatus.IMAGE_UPLOADED);
        verify(materialRepository).saveAll(TENANT_CODE, SIGN_NO, materials);
        verify(interactionLogRepository).recordSuccess(TENANT_CODE, SIGN_NO, "SIGN_RECEIVE", null, TRACE_ID);
    }

    private Sign captureSavedSign() {
        ArgumentCaptor<Sign> captor = ArgumentCaptor.forClass(Sign.class);
        verify(signRepository).insert(captor.capture());
        return captor.getValue();
    }

    private SignCommand command(List<Material> materials) {
        return new SignCommand(TENANT_CODE, TRACE_ID, EXTERNAL_APPLY_NO, "张三", null, null, null,
                null, materials, "{}");
    }
}
