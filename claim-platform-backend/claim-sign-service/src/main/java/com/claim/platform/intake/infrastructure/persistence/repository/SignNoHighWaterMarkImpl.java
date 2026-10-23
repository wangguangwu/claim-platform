package com.claim.platform.intake.infrastructure.persistence.repository;

import com.claim.platform.intake.application.exception.SignNoUnavailableException;
import com.claim.platform.intake.application.exception.SignNoUnavailableReason;
import com.claim.platform.intake.application.port.SignNoHighWaterMark;
import com.claim.platform.intake.domain.model.SignNoFormat;
import com.claim.platform.intake.infrastructure.persistence.mapper.SignNoSequenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * 从 Claim Intake 主库读取按业务日期隔离的签收号高水位。
 * 当前为单数据源；未来引入读写分离时必须显式路由主库，避免读取延迟副本。
 *
 * @author wangguangwu
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SignNoHighWaterMarkImpl implements SignNoHighWaterMark {

    private final SignNoSequenceMapper sequenceMapper;

    @Override
    public long currentMaxSequence(LocalDate businessDate) {
        String maxSignNo;
        try {
            maxSignNo = sequenceMapper.selectMaxSignNo(SignNoFormat.dateRangeStart(businessDate),
                    SignNoFormat.dateRangeEnd(businessDate));
        } catch (DataAccessException exception) {
            log.error("claim intake sign no high water mark query failed date={}", businessDate, exception);
            throw new SignNoUnavailableException(SignNoUnavailableReason.HIGH_WATER_MARK_UNAVAILABLE, exception);
        }
        if (maxSignNo == null) {
            return 0;
        }
        try {
            long sequence = SignNoFormat.parseSequence(maxSignNo);
            if (sequence < SignNoFormat.MIN_SEQUENCE || sequence > SignNoFormat.MAX_SEQUENCE) {
                throw new IllegalArgumentException("签收号高水位越界");
            }
            return sequence;
        } catch (IllegalArgumentException exception) {
            log.error("claim intake sign no high water mark invalid date={} signNo={}", businessDate, maxSignNo,
                    exception);
            throw new SignNoUnavailableException(SignNoUnavailableReason.INVALID_HIGH_WATER_MARK, exception);
        }
    }
}
