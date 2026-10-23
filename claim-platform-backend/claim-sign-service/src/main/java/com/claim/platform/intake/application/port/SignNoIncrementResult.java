package com.claim.platform.intake.application.port;

/**
 * Redis 计数器自增结果，区分成功、计数器缺失和计数器非法三种情况。
 * 合法流水号从 1 开始，不与错误标记混淆。
 *
 * @author wangguangwu
 */
public sealed interface SignNoIncrementResult {

    /**
     * 计数器存在且自增成功。
     *
     * @param sequence 当日流水号
     */
    record Allocated(long sequence) implements SignNoIncrementResult {
    }

    /**
     * 当日计数器 Key 不存在。
     */
    enum Missing implements SignNoIncrementResult {
        INSTANCE
    }

    /**
     * 当日计数器值不是正整数。
     */
    enum Invalid implements SignNoIncrementResult {
        INSTANCE
    }
}
