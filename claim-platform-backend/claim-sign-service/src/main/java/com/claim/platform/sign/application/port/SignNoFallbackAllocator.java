package com.claim.platform.sign.application.port;

import java.time.LocalDate;

/**
 * Redis 不可用时的 MySQL 降级发号端口，只生成候选流水号，不写库、不预留。
 *
 * @author wangguangwu
 */
public interface SignNoFallbackAllocator {

    /**
     * 基于主库当日最大流水号返回下一个候选流水号。
     *
     * @throws com.claim.platform.sign.application.exception.SignNoUnavailableException
     *         高水位查询失败或非法时抛出
     * @throws com.claim.platform.sign.application.exception.SignNoSequenceExhaustedException
     *         当日流水号耗尽时抛出
     */
    long nextSequence(LocalDate businessDate);
}
