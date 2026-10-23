package com.claim.platform.sign.domain.repository;

import com.claim.platform.sign.domain.model.ClaimIntake;
import com.claim.platform.sign.domain.model.ClaimIntakeDetails;

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
     * 严格插入新聚合；编号或幂等键重复时由数据库唯一键拒绝并抛出持久化异常。
     */
    void insert(ClaimIntake intake);

    /**
     * 保存状态变更后的既有聚合；实现必须遵从调用方事务边界。
     */
    void save(ClaimIntake intake);
}
