package com.claim.platform.sign.application.port;

import com.claim.platform.sign.application.result.SignNoReconcileAllocationOutcome;
import com.claim.platform.sign.application.result.SignNoReconcileOutcome;

import java.time.LocalDate;

/**
 * Redis 按业务日期共享计数器的发号端口。
 * 实现负责键、TTL、Lua 原子脚本和异常转换，应用层不感知 Redis API。
 *
 * @author wangguangwu
 */
public interface SignNoSequenceCounter {

    /**
     * 尝试对当日计数器原子自增。
     *
     * @return 成功流水号、计数器缺失或计数器非法
     */
    SignNoIncrementResult tryIncrement(LocalDate businessDate);

    /**
     * 校准确认后的计数器并立即分配下一个流水号，校准与自增在单个 Lua 脚本中原子完成。
     *
     * @param businessDate    业务日期
     * @param dbHighWaterMark MySQL 当日已使用最大流水号
     * @return 下一个可用流水号及校准前后值
     */
    SignNoReconcileAllocationOutcome reconcileAndIncrement(LocalDate businessDate, long dbHighWaterMark);

    /**
     * 只校准计数器，不消耗流水号。
     *
     * @param businessDate    业务日期
     * @param dbHighWaterMark MySQL 当日已使用最大流水号
     * @return 校准前后值
     */
    SignNoReconcileOutcome reconcile(LocalDate businessDate, long dbHighWaterMark);
}
