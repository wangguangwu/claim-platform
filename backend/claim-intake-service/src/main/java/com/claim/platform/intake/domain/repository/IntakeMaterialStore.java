package com.claim.platform.intake.domain.repository;

import com.claim.platform.intake.domain.model.IntakeMaterial;
import java.util.List;

/**
 * 定义签收材料元数据的持久化端口。
 *
 * @author wangguangwu
 */
public interface IntakeMaterialStore {

    /**
     * 保存同一上传命令中的全部材料，调用方保证已完成申请状态校验。
     */
    void saveAll(String tenantCode, String signNo, List<IntakeMaterial> materials);
}
