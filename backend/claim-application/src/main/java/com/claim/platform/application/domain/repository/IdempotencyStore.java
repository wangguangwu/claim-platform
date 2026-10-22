package com.claim.platform.application.domain.repository;

import java.util.Optional;

/**
 * 定义写入命令的幂等占位和结果回放端口。
 *
 * @author wangguangwu
 */
public interface IdempotencyStore {

    /**
     * 尝试独占当前租户、操作和幂等键；返回 false 代表已有记录。
     */
    boolean tryAcquire(String tenantCode, String operation, String idempotencyKey, String requestHash);

    /**
     * 查询已有幂等命令的摘要、状态和首次结果。
     */
    Optional<IdempotencyRecord> find(String tenantCode, String operation, String idempotencyKey);

    /**
     * 标记命令完成并保存可回放的签收编号。
     */
    void complete(String tenantCode, String operation, String idempotencyKey, String signNo);

    /**
     * 表示幂等记录的安全回放信息。
     *
     * @author wangguangwu
     */
    record IdempotencyRecord(String requestHash, String processingStatus, String resultSignNo) {
    }
}
