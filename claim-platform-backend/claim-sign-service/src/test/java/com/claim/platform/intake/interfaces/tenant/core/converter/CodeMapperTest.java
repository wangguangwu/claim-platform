package com.claim.platform.intake.interfaces.tenant.core.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.claim.platform.intake.domain.model.CertificateType;
import com.claim.platform.intake.domain.model.IntakeMaterialType;
import com.claim.platform.intake.interfaces.tenant.core.exception.IntakeCodeMappingException;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 验证租户编码到平台标准枚举的收敛规则。
 *
 * @author wangguangwu
 */
class CodeMapperTest {

    @Test
    void shouldMapCertificateCodesToPlatformEnum() {
        CodeMapper<CertificateType> mapper = new CodeMapper<>(
                "applicant.certificateType",
                Map.of("IDCARD", CertificateType.ID_CARD, "PASSPORT", CertificateType.PASSPORT));

        assertThat(mapper.map("IDCARD")).isEqualTo(CertificateType.ID_CARD);
        assertThat(mapper.map("PASSPORT")).isEqualTo(CertificateType.PASSPORT);
    }

    @Test
    void shouldMapMaterialCodesToPlatformEnum() {
        CodeMapper<IntakeMaterialType> mapper = new CodeMapper<>(
                "materials.type",
                Map.of("01", IntakeMaterialType.ID_CARD,
                        "02", IntakeMaterialType.MEDICAL_INVOICE,
                        "03", IntakeMaterialType.DIAGNOSIS_CERTIFICATE));

        assertThat(mapper.map("01")).isEqualTo(IntakeMaterialType.ID_CARD);
        assertThat(mapper.map("02")).isEqualTo(IntakeMaterialType.MEDICAL_INVOICE);
        assertThat(mapper.map("03")).isEqualTo(IntakeMaterialType.DIAGNOSIS_CERTIFICATE);
    }

    @Test
    void shouldFailWithFieldPathWhenCodeIsUnknown() {
        CodeMapper<CertificateType> mapper = new CodeMapper<>(
                "applicant.certificateType", Map.of("IDCARD", CertificateType.ID_CARD));

        IntakeCodeMappingException exception = catchThrowableOfType(
                () -> mapper.map("UNKNOWN"), IntakeCodeMappingException.class);

        assertThat(exception.fieldPath()).isEqualTo("applicant.certificateType");
        assertThat(exception.getMessage()).doesNotContain("UNKNOWN");
    }

    @Test
    void shouldFailWhenCodeIsNull() {
        CodeMapper<CertificateType> mapper = new CodeMapper<>(
                "applicant.certificateType", Map.of("IDCARD", CertificateType.ID_CARD));

        IntakeCodeMappingException exception = catchThrowableOfType(
                () -> mapper.map(null), IntakeCodeMappingException.class);

        assertThat(exception.fieldPath()).isEqualTo("applicant.certificateType");
    }

    @Test
    void shouldRejectEmptyMappingTable() {
        assertThatThrownBy(() -> new CodeMapper<CertificateType>("applicant.certificateType", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("映射表不能为空");
    }

    @Test
    void shouldRejectNullMappingEntry() {
        Map<String, CertificateType> mappings = new HashMap<>();
        mappings.put(null, CertificateType.ID_CARD);

        assertThatThrownBy(() -> new CodeMapper<>("applicant.certificateType", mappings))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("映射表包含非法项");
    }
}
