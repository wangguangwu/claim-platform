package com.claim.platform.application.usecase.command;

import com.claim.platform.application.domain.model.SignMaterial;
import java.util.List;

/**
 * 表示向已存在签收申请批量登记材料元数据的应用命令。
 *
 * @author wangguangwu
 */
public record UploadMaterialsCommand(String tenantCode, String signNo, String traceId, List<SignMaterial> materials) {
}
