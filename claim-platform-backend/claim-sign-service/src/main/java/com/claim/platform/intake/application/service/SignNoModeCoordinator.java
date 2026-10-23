package com.claim.platform.intake.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 维护实例级 Redis 发号就绪信号。
 * 初始为未就绪，实例启动后先走 MySQL 降级，直到当日高水位校准成功；
 * 该信号不代表集群级状态，跨实例一致性仍由 MySQL 高水位和全局唯一键保证。
 *
 * @author wangguangwu
 */
@Slf4j
@Component
public class SignNoModeCoordinator {

    private final AtomicBoolean redisReady = new AtomicBoolean(false);

    public boolean isRedisReady() {
        return redisReady.get();
    }

    public void markRedisReady() {
        if (redisReady.compareAndSet(false, true)) {
            log.info("claim intake sign no redis mode ready");
        }
    }

    public void markRedisUnavailable() {
        if (redisReady.compareAndSet(true, false)) {
            log.warn("claim intake sign no redis mode unavailable");
        }
    }
}
