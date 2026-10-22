package com.claim.platform.intake.application.command;

import com.claim.platform.intake.domain.model.IntakeMaterial;
import java.util.List;

/**
 * 表示向已存在签收申请批量登记材料元数据的应用命令。
 *
 * @author wangguangwu
 */
public record UploadMaterialsCommand(String tenantCode, String signNo, String traceId, List<IntakeMaterial> materials) {
}
