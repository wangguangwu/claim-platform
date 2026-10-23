package com.claim.platform.sign.interfaces.web.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.claim.platform.sign.domain.model.person.Applicant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PersonRequestConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PersonRequestConverter converter = new PersonRequestConverter(objectMapper);

    @Test
    void shouldConvertApplicantJsonWithoutDependingOnPersistenceDataObject() throws Exception {
        Applicant applicant = converter.toApplicant(objectMapper.readTree("""
                {"name":"张三","certificateType":"ID_CARD","certificateNo":"110101","mobile":"13800000000",
                 "gender":"M","birthDate":"1990-01-01","relationshipToInsured":"SELF"}
                """));

        assertThat(applicant).isEqualTo(new Applicant("张三", "ID_CARD", "110101", "13800000000", "M",
                "1990-01-01", "SELF"));
    }

    @Test
    void shouldReturnNullWhenPersonJsonIsAbsent() {
        assertThat(converter.toApplicant(null)).isNull();
    }
}
