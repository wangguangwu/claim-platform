package com.claim.platform.sign.infrastructure.persistence.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.claim.platform.sign.domain.model.Sign;
import com.claim.platform.sign.domain.model.Material;
import com.claim.platform.sign.domain.model.SignStatus;
import com.claim.platform.sign.infrastructure.persistence.dataobject.SignDO;
import com.claim.platform.sign.infrastructure.persistence.dataobject.MaterialDO;
import com.claim.platform.sign.infrastructure.persistence.converter.PersonConverterImpl;
import com.claim.platform.sign.infrastructure.persistence.dataobject.person.ApplicantInfoDO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@ContextConfiguration(classes = {SignPersistenceConverterImpl.class, PersonConverterImpl.class})
class SignPersistenceConverterTest {

    @Autowired
    private SignPersistenceConverter mapper;

    @Test
    void shouldMapSignDataObjectToDomainWithoutDatabaseTechnicalFields() {
        SignDO dataObject = new SignDO();
        dataObject.setTenantCode("tenant-a");
        dataObject.setSignNo("SIGN001");
        dataObject.setExternalApplyNo("EXT001");
        dataObject.setApplicantName("张三");
        ApplicantInfoDO applicant = new ApplicantInfoDO();
        applicant.setName("张三");
        applicant.setMobile("masked");
        dataObject.setApplicantInfo(applicant);
        dataObject.setStatus(SignStatus.IMAGE_UPLOADED);

        Sign sign = mapper.toDomain(dataObject);

        assertThat(sign.tenantCode()).isEqualTo("tenant-a");
        assertThat(sign.signNo()).isEqualTo("SIGN001");
        assertThat(sign.status()).isEqualTo(SignStatus.IMAGE_UPLOADED);
        assertThat(sign.applicant()).isNotNull();
        assertThat(sign.applicant().name()).isEqualTo("张三");
    }

    @Test
    void shouldMapDomainSnapshotToNewDataObjectWithoutIdAndVersion() {
        Sign sign = Sign.create("tenant-a", "SIGN001", "EXT001", "张三", null,
                null, null, null, null);

        SignDO dataObject = mapper.toDO(sign);

        assertThat(dataObject.getTenantCode()).isEqualTo("tenant-a");
        assertThat(dataObject.getSignNo()).isEqualTo("SIGN001");
        assertThat(dataObject.getStatus()).isEqualTo(SignStatus.IMAGE_NOT_UPLOAD);
        assertThat(dataObject.getId()).isNull();
        assertThat(dataObject.getVersion()).isNull();
    }

    @Test
    void shouldMapMaterialDataObjectToDomainValueObject() {
        MaterialDO dataObject = new MaterialDO("tenant-a", "SIGN001", "BATCH001", 1, "ID_CARD",
                "身份证正面", "storage://material/1", null);

        Material material = mapper.toDomain(dataObject);

        assertThat(material.batchNo()).isEqualTo("BATCH001");
        assertThat(material.name()).isEqualTo("身份证正面");
    }
}
