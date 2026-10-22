package com.claim.platform.intake.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.claim.platform.intake.domain.model.ClaimIntake;
import com.claim.platform.intake.domain.model.IntakeMaterial;
import com.claim.platform.intake.domain.model.IntakeStatus;
import com.claim.platform.intake.infrastructure.persistence.ClaimIntakeDO;
import com.claim.platform.intake.infrastructure.persistence.IntakeMaterialDO;
import com.claim.platform.intake.infrastructure.persistence.PersonConverterImpl;
import com.claim.platform.intake.infrastructure.persistence.person.ApplicantInfoDO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@ContextConfiguration(classes = {ClaimIntakePersistenceMapperImpl.class, PersonConverterImpl.class})
class ClaimIntakePersistenceMapperTest {

    @Autowired
    private ClaimIntakePersistenceMapper mapper;

    @Test
    void shouldMapIntakeDataObjectToDomainWithoutDatabaseTechnicalFields() {
        ClaimIntakeDO dataObject = new ClaimIntakeDO();
        dataObject.setTenantCode("tenant-a");
        dataObject.setSignNo("SIGN001");
        dataObject.setExternalApplyNo("EXT001");
        dataObject.setApplicantName("张三");
        ApplicantInfoDO applicant = new ApplicantInfoDO();
        applicant.setName("张三");
        applicant.setMobile("masked");
        dataObject.setApplicantInfo(applicant);
        dataObject.setStatus(IntakeStatus.IMAGE_UPLOADED);

        ClaimIntake intake = mapper.toDomain(dataObject);

        assertThat(intake.tenantCode()).isEqualTo("tenant-a");
        assertThat(intake.signNo()).isEqualTo("SIGN001");
        assertThat(intake.status()).isEqualTo(IntakeStatus.IMAGE_UPLOADED);
        assertThat(intake.applicant()).isNotNull();
        assertThat(intake.applicant().name()).isEqualTo("张三");
    }

    @Test
    void shouldMapDomainSnapshotToNewDataObjectWithoutIdAndVersion() {
        ClaimIntake intake = ClaimIntake.create("tenant-a", "SIGN001", "EXT001", "张三", null,
                null, null, null, null);

        ClaimIntakeDO dataObject = mapper.toDO(intake);

        assertThat(dataObject.getTenantCode()).isEqualTo("tenant-a");
        assertThat(dataObject.getSignNo()).isEqualTo("SIGN001");
        assertThat(dataObject.getStatus()).isEqualTo(IntakeStatus.IMAGE_NOT_UPLOAD);
        assertThat(dataObject.getId()).isNull();
        assertThat(dataObject.getVersion()).isNull();
    }

    @Test
    void shouldMapMaterialDataObjectToDomainValueObject() {
        IntakeMaterialDO dataObject = new IntakeMaterialDO("tenant-a", "SIGN001", "BATCH001", 1, "ID_CARD",
                "身份证正面", "storage://material/1", null);

        IntakeMaterial material = mapper.toDomain(dataObject);

        assertThat(material.batchNo()).isEqualTo("BATCH001");
        assertThat(material.name()).isEqualTo("身份证正面");
    }
}
