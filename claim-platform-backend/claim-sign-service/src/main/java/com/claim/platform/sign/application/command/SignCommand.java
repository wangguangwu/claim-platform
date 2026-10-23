package com.claim.platform.sign.application.command;

import com.claim.platform.sign.domain.model.Material;
import com.claim.platform.sign.domain.model.person.AccidentPerson;
import com.claim.platform.sign.domain.model.person.Applicant;
import com.claim.platform.sign.domain.model.person.MainInsured;
import com.claim.platform.sign.domain.model.person.Payee;

import java.util.List;

/**
 * 表示从已鉴别租户接收签收申请的应用命令；请求体不携带租户字段。
 * 四类人员以强类型 {@code Person} 子类型承载，由 Web Adapter 在边界内完成 JSON 到领域的转换。
 * 材料只承载元数据，随标准命令与申请在同一事务内创建。
 *
 * @author wangguangwu
 */
public record SignCommand(String tenantCode, String traceId, String externalApplyNo,
                                  String applicantName, Applicant applicant, AccidentPerson accidentPerson,
                                  MainInsured mainInsured, Payee payee, List<Material> materials,
                                  String extJson) {

    public SignCommand {
        materials = materials == null ? List.of() : List.copyOf(materials);
    }
}
