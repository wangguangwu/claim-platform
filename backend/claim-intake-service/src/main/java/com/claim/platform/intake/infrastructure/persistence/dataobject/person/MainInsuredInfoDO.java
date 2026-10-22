package com.claim.platform.intake.infrastructure.persistence.dataobject.person;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主被保险人持久化对象，对应 {@code t_claim_intake.main_insured_info} JSON 列。
 *
 * @author wangguangwu
 */
@Data
@NoArgsConstructor
public class MainInsuredInfoDO extends PersonInfoDO {

    private String policyNo;
    private String beneficiaryRelation;
}
