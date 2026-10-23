package com.claim.platform.sign.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.claim.platform.sign.domain.exception.InvalidSignStatusException;
import org.junit.jupiter.api.Test;

class SignTest {

    @Test
    void shouldCreateSignInImageNotUploadStatus() {
        Sign sign = Sign.create("tenant-a", "SIGN001", "EXT001", "张三", null,
                null, null, null, null);

        assertThat(sign.status()).isEqualTo(SignStatus.IMAGE_NOT_UPLOAD);
    }

    @Test
    void shouldRegisterMaterialsFromImageNotUploadStatus() {
        Sign sign = Sign.create("tenant-a", "SIGN001", "EXT001", "张三", null,
                null, null, null, null);

        sign.registerMaterials();

        assertThat(sign.status()).isEqualTo(SignStatus.IMAGE_UPLOADED);
    }

    @Test
    void shouldRegisterMaterialsFromIssueCaseStatusForSupplement() {
        Sign sign = Sign.restore("tenant-a", "SIGN001", "EXT001", "张三", null,
                null, null, null, null, SignStatus.ISSUE_CASE);

        sign.registerMaterials();

        assertThat(sign.status()).isEqualTo(SignStatus.IMAGE_UPLOADED);
    }

    @Test
    void shouldRejectMaterialRegistrationAfterAuditSyncSuccess() {
        Sign sign = Sign.restore("tenant-a", "SIGN001", "EXT001", "张三", null,
                null, null, null, null, SignStatus.SYNC_AUDIT_SUCCESS);

        assertThatThrownBy(sign::registerMaterials).isInstanceOf(InvalidSignStatusException.class);
    }
}
