package com.claim.platform.sign.infrastructure.persistence.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.claim.platform.sign.domain.model.person.AccidentPerson;
import com.claim.platform.sign.domain.model.person.Applicant;
import com.claim.platform.sign.domain.model.person.MainInsured;
import com.claim.platform.sign.domain.model.person.Payee;
import com.claim.platform.sign.infrastructure.persistence.dataobject.person.AccidentPersonInfoDO;
import com.claim.platform.sign.infrastructure.persistence.dataobject.person.ApplicantInfoDO;
import com.claim.platform.sign.infrastructure.persistence.dataobject.person.MainInsuredInfoDO;
import com.claim.platform.sign.infrastructure.persistence.dataobject.person.PayeeInfoDO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * 验证 PersonConverter 在四类人员持久化对象与领域子类型间的双向转换与空值处理。
 *
 * @author wangguangwu
 */
@SpringJUnitConfig
@ContextConfiguration(classes = PersonConverterImpl.class)
class PersonConverterTest {

    @Autowired
    private PersonConverter converter;

    @Test
    void shouldConvertApplicantInfoDoToDomainWithRoleField() {
        ApplicantInfoDO source = new ApplicantInfoDO();
        source.setName("张三");
        source.setCertificateType("ID_CARD");
        source.setCertificateNo("ID-001");
        source.setMobile("13800000000");
        source.setGender("M");
        source.setBirthDate("1990-01-01");
        source.setRelationshipToInsured("SELF");

        Applicant applicant = converter.toApplicant(source);

        assertThat(applicant.name()).isEqualTo("张三");
        assertThat(applicant.certificateNo()).isEqualTo("ID-001");
        assertThat(applicant.relationshipToInsured()).isEqualTo("SELF");
    }

    @Test
    void shouldConvertAccidentPersonInfoDoToDomain() {
        AccidentPersonInfoDO source = new AccidentPersonInfoDO();
        source.setName("李四");
        source.setAccidentDate("2024-06-01");
        source.setAccidentType("TRAFFIC");

        AccidentPerson person = converter.toAccidentPerson(source);

        assertThat(person.name()).isEqualTo("李四");
        assertThat(person.accidentDate()).isEqualTo("2024-06-01");
        assertThat(person.accidentType()).isEqualTo("TRAFFIC");
    }

    @Test
    void shouldConvertMainInsuredInfoDoToDomain() {
        MainInsuredInfoDO source = new MainInsuredInfoDO();
        source.setName("王五");
        source.setPolicyNo("POL-001");
        source.setBeneficiaryRelation("SPOUSE");

        MainInsured person = converter.toMainInsured(source);

        assertThat(person.policyNo()).isEqualTo("POL-001");
        assertThat(person.beneficiaryRelation()).isEqualTo("SPOUSE");
    }

    @Test
    void shouldConvertPayeeInfoDoToDomain() {
        PayeeInfoDO source = new PayeeInfoDO();
        source.setName("赵六");
        source.setBankName("ICBC");
        source.setBankAccount("6222000000000000");
        source.setPayeeRelation("SELF");

        Payee person = converter.toPayee(source);

        assertThat(person.bankName()).isEqualTo("ICBC");
        assertThat(person.bankAccount()).isEqualTo("6222000000000000");
        assertThat(person.payeeRelation()).isEqualTo("SELF");
    }

    @Test
    void shouldConvertDomainToInfoDoRoundTrip() {
        Applicant applicant = new Applicant("张三", "ID_CARD", "ID-001", "13800000000", "M", "1990-01-01", "SELF");

        ApplicantInfoDO dataObject = converter.toApplicantInfoDO(applicant);

        assertThat(dataObject.getName()).isEqualTo("张三");
        assertThat(dataObject.getCertificateNo()).isEqualTo("ID-001");
        assertThat(dataObject.getRelationshipToInsured()).isEqualTo("SELF");
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        assertThat(converter.toApplicant(null)).isNull();
        assertThat(converter.toAccidentPerson(null)).isNull();
        assertThat(converter.toMainInsured(null)).isNull();
        assertThat(converter.toPayee(null)).isNull();
        assertThat(converter.toApplicantInfoDO(null)).isNull();
        assertThat(converter.toAccidentPersonInfoDO(null)).isNull();
        assertThat(converter.toMainInsuredInfoDO(null)).isNull();
        assertThat(converter.toPayeeInfoDO(null)).isNull();
    }
}
