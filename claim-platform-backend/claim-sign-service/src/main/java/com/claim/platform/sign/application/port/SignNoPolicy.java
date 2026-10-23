package com.claim.platform.sign.application.port;

/**
 * 应用层可见的签收号策略开关，避免应用服务依赖基础设施配置类。
 *
 * @author wangguangwu
 */
public interface SignNoPolicy {

    /**
     * Redis 不可用时是否允许降级到 MySQL 发号。
     */
    boolean isFallbackEnabled();

    /**
     * 唯一冲突后的最大创建尝试次数。
     */
    int getMaxAttempts();
}
