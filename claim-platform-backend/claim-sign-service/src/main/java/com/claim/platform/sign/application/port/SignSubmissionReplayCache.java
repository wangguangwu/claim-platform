package com.claim.platform.sign.application.port;

import java.util.Optional;

/**
 * 定义签收幂等键到平台签收号的定位缓存端口。
 * 缓存只优化定位路径，申请状态和最终幂等事实始终以 MySQL 为准。
 *
 * @author wangguangwu
 */
public interface SignSubmissionReplayCache {

    /**
     * 按租户和外部申请编号读取已缓存的签收编号。
     * Redis 不可用时实现应按缓存未命中处理，不得影响签收主流程。
     *
     * @param tenantCode      当前可信租户
     * @param externalApplyNo 外部申请编号
     * @return 已缓存的签收编号；未命中或缓存不可用时为空
     */
    Optional<String> findSignNo(String tenantCode, String externalApplyNo);

    /**
     * 以最佳努力写入幂等键到签收编号的映射。
     * 缓存写入失败不得影响已提交的数据库事务。
     *
     * @param tenantCode      当前可信租户
     * @param externalApplyNo 外部申请编号
     * @param signNo          平台签收编号
     */
    void put(String tenantCode, String externalApplyNo, String signNo);

    /**
     * 删除无法定位同租户申请的失效映射。
     * Redis 不可用时实现应保持无副作用。
     *
     * @param tenantCode      当前可信租户
     * @param externalApplyNo 外部申请编号
     */
    void evict(String tenantCode, String externalApplyNo);
}
