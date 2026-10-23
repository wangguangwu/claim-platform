package com.claim.platform.intake.application.port;

/**
 * 恢复锁句柄。关闭时只释放当前线程持有的锁，所有权丢失时不强制删除 Redis 键。
 *
 * @author wangguangwu
 */
public interface SignNoRecoveryLockHandle extends AutoCloseable {

    LockAcquireStatus status();

    @Override
    void close();
}
