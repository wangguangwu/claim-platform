package com.claim.platform.intake.application.exception;

/**
 * 签收号暂时不可用的内部原因，只用于服务端日志、指标和降级决策，不返回调用方。
 *
 * @author wangguangwu
 */
public enum SignNoUnavailableReason {

    REDIS_UNAVAILABLE,
    RECOVERY_LOCK_UNAVAILABLE,
    RECOVERY_LOCK_TIMEOUT,
    HIGH_WATER_MARK_UNAVAILABLE,
    INVALID_HIGH_WATER_MARK,
    FALLBACK_DISABLED,
    FALLBACK_UNAVAILABLE,
    RETRY_EXHAUSTED;

    /**
     * 判断失败是否来自 Redis 侧；只有 Redis 侧失败才应把实例标记为不可用并降级。
     */
    public boolean isRedisSide() {
        return this == REDIS_UNAVAILABLE || this == RECOVERY_LOCK_UNAVAILABLE || this == RECOVERY_LOCK_TIMEOUT;
    }
}
