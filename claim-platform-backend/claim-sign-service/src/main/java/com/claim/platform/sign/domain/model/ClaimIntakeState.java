package com.claim.platform.sign.domain.model;

import com.claim.platform.sign.domain.model.person.AccidentPerson;
import com.claim.platform.sign.domain.model.person.Applicant;
import com.claim.platform.sign.domain.model.person.MainInsured;
import com.claim.platform.sign.domain.model.person.Payee;

/**
 * 表示从持久化层恢复签收申请聚合所需的完整领域状态，不包含数据库主键、乐观锁版本等基础设施属性。
 * 四类人员以强类型 {@code Person} 子类型承载，由 PersonConverter 在基础设施层装配。
 *
 * @author wangguangwu
 */
public record ClaimIntakeState(String tenantCode, String signNo, String externalApplyNo, String applicantName,
                               Applicant applicant, AccidentPerson accidentPerson, MainInsured mainInsured,
                               Payee payee, String extJson, IntakeStatus status) {
}
