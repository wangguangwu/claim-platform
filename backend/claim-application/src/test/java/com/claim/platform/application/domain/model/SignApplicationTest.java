package com.claim.platform.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.claim.platform.application.domain.exception.InvalidApplicationStatusException;
import org.junit.jupiter.api.Test;

class SignApplicationTest {

    @Test
    void shouldCreateApplicationInImageNotUploadStatus() {
        SignApplication application = SignApplication.create("tenant-a", "SIGN001", "EXT001", "张三", null,
                null, null, null, null);

        assertThat(application.status()).isEqualTo(SignApplicationStatus.IMAGE_NOT_UPLOAD);
    }

    @Test
    void shouldRegisterMaterialsFromImageNotUploadStatus() {
        SignApplication application = SignApplication.create("tenant-a", "SIGN001", "EXT001", "张三", null,
                null, null, null, null);

        application.registerMaterials();

        assertThat(application.status()).isEqualTo(SignApplicationStatus.IMAGE_UPLOADED);
    }

    @Test
    void shouldRegisterMaterialsFromIssueCaseStatusForSupplement() {
        SignApplication application = SignApplication.restore("tenant-a", "SIGN001", "EXT001", "张三", null,
                null, null, null, null, SignApplicationStatus.ISSUE_CASE);

        application.registerMaterials();

        assertThat(application.status()).isEqualTo(SignApplicationStatus.IMAGE_UPLOADED);
    }

    @Test
    void shouldRejectMaterialRegistrationAfterAuditSyncSuccess() {
        SignApplication application = SignApplication.restore("tenant-a", "SIGN001", "EXT001", "张三", null,
                null, null, null, null, SignApplicationStatus.SYNC_AUDIT_SUCCESS);

        assertThatThrownBy(application::registerMaterials).isInstanceOf(InvalidApplicationStatusException.class);
    }
}
