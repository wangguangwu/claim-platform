package com.claim.platform.sign.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.claim.platform.sign.application.exception.SignNoSequenceExhaustedException;
import com.claim.platform.sign.application.exception.SignNoUnavailableException;
import com.claim.platform.sign.application.exception.SignNoUnavailableReason;
import com.claim.platform.sign.application.port.SignNoHighWaterMark;
import com.claim.platform.sign.domain.model.SignNoFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

/**
 * 验证 MySQL 降级发号只生成候选流水号并遵守容量上限。
 *
 * @author wangguangwu
 */
@ExtendWith(MockitoExtension.class)
class MysqlSignNoFallbackAllocatorTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 10, 23);

    @Mock
    private SignNoHighWaterMark highWaterMark;
    @InjectMocks
    private MysqlSignNoFallbackAllocator fallbackAllocator;

    @Test
    void shouldReturnFirstSequenceWhenNoRowsExist() {
        given(highWaterMark.currentMaxSequence(BUSINESS_DATE)).willReturn(0L);

        assertThat(fallbackAllocator.nextSequence(BUSINESS_DATE)).isEqualTo(1);
    }

    @Test
    void shouldReturnNextSequenceAfterHighWaterMark() {
        given(highWaterMark.currentMaxSequence(BUSINESS_DATE)).willReturn(10L);

        assertThat(fallbackAllocator.nextSequence(BUSINESS_DATE)).isEqualTo(11);
    }

    @Test
    void shouldRejectWhenHighWaterMarkReachesUpperBound() {
        given(highWaterMark.currentMaxSequence(BUSINESS_DATE))
                .willReturn(SignNoFormat.MAX_SEQUENCE);

        assertThatThrownBy(() -> fallbackAllocator.nextSequence(BUSINESS_DATE))
                .isInstanceOf(SignNoSequenceExhaustedException.class);
    }

    @Test
    void shouldPropagateHighWaterMarkFailure() {
        SignNoUnavailableException failure =
                new SignNoUnavailableException(SignNoUnavailableReason.HIGH_WATER_MARK_UNAVAILABLE);
        given(highWaterMark.currentMaxSequence(BUSINESS_DATE)).willThrow(failure);

        assertThatThrownBy(() -> fallbackAllocator.nextSequence(BUSINESS_DATE)).isSameAs(failure);
    }
}
