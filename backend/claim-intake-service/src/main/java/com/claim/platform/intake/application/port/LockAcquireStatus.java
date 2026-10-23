package com.claim.platform.intake.application.port;

/**
 * 表示签收提交业务锁的获取结果。
 *
 * @author wangguangwu
 */
public enum LockAcquireStatus {

    /**
     * 当前请求已持有锁。
     */
    ACQUIRED,

    /**
     * 在配置的等待时间内未获取锁。
     */
    TIMEOUT,

    /**
     * Redis 连接或锁操作不可用，调用方应按降级路径继续。
     */
    UNAVAILABLE
}
