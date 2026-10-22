package com.claim.platform.intake.domain.repository;

import com.claim.platform.intake.domain.model.ClaimIntake;
import com.claim.platform.intake.domain.model.ClaimIntakeDetails;
import java.util.Optional;

/**
 * 定义签收申请聚合的持久化端口，领域层不感知 MyBatis-Plus 或数据库实现。
 *
 * @author wangguangwu
 */
public interface ClaimIntakeStore {

    /**
     * 在当前租户范围内按签收编号加载聚合。
     *
     * @return 不存在时为空
     */
    Optional<ClaimIntake> findByTenantAndSignNo(String tenantCode, String signNo);

    /**
     * 按租户和外部申请编号查找既有签收事实，用于天然业务幂等回放。
     */
    Optional<ClaimIntake> findByTenantAndExternalApplyNo(String tenantCode, String externalApplyNo);

    /**
     * 加载申请和其未删除材料，用于租户隔离查询。
     *
     * @return 不存在时为空
     */
    Optional<ClaimIntakeDetails> findDetailsByTenantAndSignNo(String tenantCode, String signNo);

    /**
     * 判断外部申请编号是否已在当前租户形成签收事实。
     */
    boolean existsByTenantAndExternalApplyNo(String tenantCode, String externalApplyNo);

    /**
     * 保存创建或状态变更后的聚合；实现必须遵从调用方事务边界。
     */
    void save(ClaimIntake intake);
}
