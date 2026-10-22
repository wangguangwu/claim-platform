package com.claim.platform.intake.infrastructure.persistence.dataobject.person;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 申请人持久化对象，对应 {@code t_claim_intake.applicant_info} JSON 列。
 *
 * @author wangguangwu
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ApplicantInfoDO extends PersonInfoDO {

    private String relationshipToInsured;
}
