package com.claim.platform.sign.application.service;

import com.claim.platform.sign.application.exception.SignNoSequenceExhaustedException;
import com.claim.platform.sign.application.port.SignNoFallbackAllocator;
import com.claim.platform.sign.application.port.SignNoHighWaterMark;
import com.claim.platform.sign.domain.model.SignNo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Redis 不可用时基于 MySQL 高水位生成候选流水号。
 * 不获取恢复锁、不写 Redis、不插入占位行；并发冲突由数据库全局唯一键拒绝。
 *
 * @author wangguangwu
 */
@Component
@RequiredArgsConstructor
public class MysqlSignNoFallbackAllocator implements SignNoFallbackAllocator {

    private final SignNoHighWaterMark highWaterMark;

    @Override
    public long nextSequence(LocalDate businessDate) {
        long dbMax = highWaterMark.currentMaxSequence(businessDate);
        if (dbMax >= SignNo.MAX_SEQUENCE) {
            throw new SignNoSequenceExhaustedException();
        }
        return dbMax + 1;
    }
}
