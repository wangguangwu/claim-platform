package com.claim.platform.intake.application.result;

/**
 * 请求路径恢复并立即发号的结果，同时携带校准前后值用于恢复日志。
 *
 * @param sequence      分配到的流水号
 * @param counterBefore 校准前计数器值
 * @param counterAfter  校准后、自增前的计数器值
 * @author wangguangwu
 */
public record SignNoReconcileAllocationOutcome(long sequence, long counterBefore, long counterAfter) {
}
