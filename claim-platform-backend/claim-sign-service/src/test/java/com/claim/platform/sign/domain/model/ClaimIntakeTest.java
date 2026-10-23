package com.claim.platform.sign.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.claim.platform.sign.domain.exception.InvalidIntakeStatusException;
import org.junit.jupiter.api.Test;

class ClaimIntakeTest {

    @Test
    void shouldCreateIntakeInImageNotUploadStatus() {
        ClaimIntake intake = ClaimIntake.create("tenant-a", "SIGN001", "EXT001", "张三", null,
                null, null, null, null);

        assertThat(intake.status()).isEqualTo(IntakeStatus.IMAGE_NOT_UPLOAD);
    }

    @Test
    void shouldRegisterMaterialsFromImageNotUploadStatus() {
        ClaimIntake intake = ClaimIntake.create("tenant-a", "SIGN001", "EXT001", "张三", null,
                null, null, null, null);

        intake.registerMaterials();

        assertThat(intake.status()).isEqualTo(IntakeStatus.IMAGE_UPLOADED);
    }

    @Test
    void shouldRegisterMaterialsFromIssueCaseStatusForSupplement() {
        ClaimIntake intake = ClaimIntake.restore("tenant-a", "SIGN001", "EXT001", "张三", null,
                null, null, null, null, IntakeStatus.ISSUE_CASE);

        intake.registerMaterials();

        assertThat(intake.status()).isEqualTo(IntakeStatus.IMAGE_UPLOADED);
    }

    @Test
    void shouldRejectMaterialRegistrationAfterAuditSyncSuccess() {
        ClaimIntake intake = ClaimIntake.restore("tenant-a", "SIGN001", "EXT001", "张三", null,
                null, null, null, null, IntakeStatus.SYNC_AUDIT_SUCCESS);

        assertThatThrownBy(intake::registerMaterials).isInstanceOf(InvalidIntakeStatusException.class);
    }
}
