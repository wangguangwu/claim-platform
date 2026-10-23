package com.claim.platform.sign.application.port;

import java.time.LocalDate;

/**
 * 按业务日期隔离的 Redis 恢复锁，串行化 MySQL 高水位查询与计数器校准。
 *
 * @author wangguangwu
 */
public interface SignNoRecoveryLock {

    /**
     * 在限定等待时间内尝试获取指定业务日期的恢复锁。
     *
     * @return 获取结果；未获取时句柄在关闭时不做任何操作
     */
    SignNoRecoveryLockHandle tryAcquire(LocalDate businessDate);
}
