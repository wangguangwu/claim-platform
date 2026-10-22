package com.claim.platform.intake.application.command;

import com.claim.platform.intake.domain.model.person.AccidentPerson;
import com.claim.platform.intake.domain.model.person.Applicant;
import com.claim.platform.intake.domain.model.person.MainInsured;
import com.claim.platform.intake.domain.model.person.Payee;

/**
 * 表示从已鉴别租户接收签收申请的应用命令；请求体不携带租户字段。
 * 四类人员以强类型 {@code Person} 子类型承载，由 Web Adapter 在边界内完成 JSON 到领域的转换。
 *
 * @author wangguangwu
 */
public record SubmitIntakeCommand(String tenantCode, String traceId, String externalApplyNo,
                                           String applicantName, Applicant applicant, AccidentPerson accidentPerson,
                                           MainInsured mainInsured, Payee payee, String extJson) {
}
