package com.claim.platform.intake.infrastructure.persistence.person;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 出险人持久化对象，对应 {@code t_claim_intake.accident_person_info} JSON 列。
 *
 * @author wangguangwu
 */
@Data
@NoArgsConstructor
public class AccidentPersonInfoDO extends PersonInfoDO {

    private String accidentDate;
    private String accidentType;
}
