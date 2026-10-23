package com.claim.platform.sign.application.result;

/**
 * Redis 计数器校准前后值，用于后台校准日志和序列差值指标。
 *
 * @param counterBefore 校准前计数器值
 * @param counterAfter  校准后计数器值
 * @author wangguangwu
 */
public record SignNoReconcileOutcome(long counterBefore, long counterAfter) {
}
