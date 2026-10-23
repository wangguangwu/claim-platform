package com.claim.platform.intake.application.result;

import com.claim.platform.intake.application.exception.SignNoUnavailableReason;

import java.time.LocalDate;

/**
 * 单次签收号校准结果。
 * 未知的高水位或计数器值使用 {@code -1}；正序列差值表示 Redis 落后 MySQL。
 *
 * @param businessDate    业务日期
 * @param dbHighWaterMark MySQL 当日最大流水号，未知时为 -1
 * @param counterBefore   校准前计数器值，未知时为 -1
 * @param counterAfter    校准后计数器值，未知时为 -1
 * @param changed         计数器是否发生变化
 * @param status          校准状态
 * @param failureReason   失败原因，成功时为 null
 * @author wangguangwu
 */
public record SignNoReconciliationResult(
        LocalDate businessDate,
        long dbHighWaterMark,
        long counterBefore,
        long counterAfter,
        boolean changed,
        ReconciliationStatus status,
        SignNoUnavailableReason failureReason) {

    public static SignNoReconciliationResult success(LocalDate businessDate, long dbHighWaterMark,
                                                     long counterBefore, long counterAfter) {
        return new SignNoReconciliationResult(businessDate, dbHighWaterMark, counterBefore, counterAfter,
                counterBefore != counterAfter, ReconciliationStatus.SUCCESS, null);
    }

    public static SignNoReconciliationResult skipped(LocalDate businessDate, SignNoUnavailableReason reason) {
        return new SignNoReconciliationResult(businessDate, -1, -1, -1, false,
                ReconciliationStatus.SKIPPED, reason);
    }

    /**
     * 因已有校准轮次运行而跳过，不涉及具体不可用原因。
     */
    public static SignNoReconciliationResult skipped(LocalDate businessDate) {
        return new SignNoReconciliationResult(businessDate, -1, -1, -1, false,
                ReconciliationStatus.SKIPPED, null);
    }

    public static SignNoReconciliationResult failed(LocalDate businessDate, SignNoUnavailableReason reason) {
        return new SignNoReconciliationResult(businessDate, -1, -1, -1, false,
                ReconciliationStatus.FAILED, reason);
    }

    /**
     * 返回 MySQL 高水位与校准前计数器的差值；任一值未知时返回 -1。
     */
    public long sequenceGap() {
        if (dbHighWaterMark < 0 || counterBefore < 0) {
            return -1;
        }
        return dbHighWaterMark - counterBefore;
    }
}
