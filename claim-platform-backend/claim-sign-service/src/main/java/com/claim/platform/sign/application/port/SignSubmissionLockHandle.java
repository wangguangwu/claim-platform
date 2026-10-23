package com.claim.platform.sign.application.port;

/**
 * 表示一次业务锁获取结果及其释放责任。
 * 调用方必须使用 try-with-resources，确保申请成功或异常时都会释放当前请求持有的锁。
 *
 * @author wangguangwu
 */
public interface SignSubmissionLockHandle extends AutoCloseable {

    /**
     * 返回锁获取结果。
     */
    LockAcquireStatus status();

    /**
     * 释放当前请求持有的锁；超时或降级句柄不执行任何操作。
     */
    @Override
    void close();
}
