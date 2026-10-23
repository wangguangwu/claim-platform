package com.claim.platform.intake.application.port;

/**
 * 定义按租户和外部申请编号互斥的短期业务锁端口。
 * 锁只降低并发冲突，申请幂等的最终事实仍由 MySQL 唯一键保证。
 *
 * @author wangguangwu
 */
public interface IntakeSubmissionLock {

    /**
     * 尝试获取以 {@code tenantCode + externalApplyNo} 为业务键的锁。
     * 实现必须区分等待超时与 Redis 不可用，且不得让不同租户互相阻塞。
     *
     * @param tenantCode      当前可信租户
     * @param externalApplyNo 外部申请编号
     * @return 锁获取结果及释放句柄
     */
    IntakeSubmissionLockHandle tryAcquire(String tenantCode, String externalApplyNo);
}
