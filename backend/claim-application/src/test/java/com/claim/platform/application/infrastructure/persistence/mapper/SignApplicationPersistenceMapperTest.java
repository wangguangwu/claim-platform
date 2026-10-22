package com.claim.platform.application.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.claim.platform.application.domain.model.SignApplication;
import com.claim.platform.application.domain.model.SignApplicationStatus;
import com.claim.platform.application.domain.model.SignMaterial;
import com.claim.platform.application.infrastructure.persistence.SignApplicationDO;
import com.claim.platform.application.infrastructure.persistence.SignMaterialDO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@ContextConfiguration(classes = SignApplicationPersistenceMapperImpl.class)
class SignApplicationPersistenceMapperTest {

    @Autowired
    private SignApplicationPersistenceMapper mapper;

    @Test
    void shouldMapApplicationDataObjectToDomainWithoutDatabaseTechnicalFields() {
        SignApplicationDO dataObject = new SignApplicationDO("tenant-a", "SIGN001", "EXT001", "张三",
                "{\"mobile\":\"masked\"}", null, null, null, null, SignApplicationStatus.IMAGE_UPLOADED);

        SignApplication application = mapper.toDomain(dataObject);

        assertThat(application.tenantCode()).isEqualTo("tenant-a");
        assertThat(application.signNo()).isEqualTo("SIGN001");
        assertThat(application.status()).isEqualTo(SignApplicationStatus.IMAGE_UPLOADED);
    }

    @Test
    void shouldMapDomainSnapshotToNewDataObjectWithoutIdAndVersion() {
        SignApplication application = SignApplication.create("tenant-a", "SIGN001", "EXT001", "张三", null,
                null, null, null, null);

        SignApplicationDO dataObject = mapper.toDO(application);

        assertThat(dataObject.getTenantCode()).isEqualTo("tenant-a");
        assertThat(dataObject.getSignNo()).isEqualTo("SIGN001");
        assertThat(dataObject.getStatus()).isEqualTo(SignApplicationStatus.IMAGE_NOT_UPLOAD);
        assertThat(dataObject.getId()).isNull();
        assertThat(dataObject.getVersion()).isNull();
    }

    @Test
    void shouldMapMaterialDataObjectToDomainValueObject() {
        SignMaterialDO dataObject = new SignMaterialDO("tenant-a", "SIGN001", "BATCH001", 1, "ID_CARD",
                "身份证正面", "storage://material/1", null);

        SignMaterial material = mapper.toDomain(dataObject);

        assertThat(material.batchNo()).isEqualTo("BATCH001");
        assertThat(material.name()).isEqualTo("身份证正面");
    }
}
