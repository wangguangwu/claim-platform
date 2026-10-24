package com.claim.platform.sign.application.command;

import com.claim.platform.sign.domain.model.Material;

import java.util.List;

/**
 * 表示从已鉴别租户接收签收申请的应用命令；请求体不携带租户字段。
 * 人员明细只在 {@code extJson} 中透传，领域层不承载人员对象。
 * 材料只承载元数据，随标准命令与申请在同一事务内创建。
 *
 * @author wangguangwu
 */
public record SignCommand(String tenantCode, String traceId, String externalApplyNo, String applicantName,
                                  List<Material> materials, String extJson) {

    public SignCommand {
        materials = materials == null ? List.of() : List.copyOf(materials);
    }
}
