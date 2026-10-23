package com.claim.platform.intake.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.claim.platform.intake.application.exception.SignNoUnavailableException;
import com.claim.platform.intake.application.exception.SignNoUnavailableReason;
import com.claim.platform.intake.infrastructure.persistence.mapper.SignNoSequenceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDate;

/**
 * 验证按日期范围读取高水位、空值归零和异常分类。
 *
 * @author wangguangwu
 */
@ExtendWith(MockitoExtension.class)
class SignNoHighWaterMarkImplTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 10, 23);

    @Mock
    private SignNoSequenceMapper sequenceMapper;
    @InjectMocks
    private SignNoHighWaterMarkImpl highWaterMark;

    @Test
    void shouldQueryHalfOpenDateRange() {
        given(sequenceMapper.selectMaxSignNo("S20261023", "S20261024")).willReturn(null);

        assertThat(highWaterMark.currentMaxSequence(BUSINESS_DATE)).isZero();

        verify(sequenceMapper).selectMaxSignNo("S20261023", "S20261024");
    }

    @Test
    void shouldParseMaximumSequence() {
        given(sequenceMapper.selectMaxSignNo("S20261023", "S20261024"))
                .willReturn("S2026102300000123");

        assertThat(highWaterMark.currentMaxSequence(BUSINESS_DATE)).isEqualTo(123);
    }

    @Test
    void shouldRejectNonCurrentFormatHighWaterMark() {
        given(sequenceMapper.selectMaxSignNo("S20261023", "S20261024")).willReturn("SIGN0000000000000000001");

        assertThatThrownBy(() -> highWaterMark.currentMaxSequence(BUSINESS_DATE))
                .isInstanceOf(SignNoUnavailableException.class)
                .satisfies(exception -> assertThat(((SignNoUnavailableException) exception).reason())
                        .isEqualTo(SignNoUnavailableReason.INVALID_HIGH_WATER_MARK));
    }

    @Test
    void shouldRejectZeroSequenceHighWaterMark() {
        given(sequenceMapper.selectMaxSignNo("S20261023", "S20261024"))
                .willReturn("S2026102300000000");

        assertThatThrownBy(() -> highWaterMark.currentMaxSequence(BUSINESS_DATE))
                .isInstanceOf(SignNoUnavailableException.class)
                .satisfies(exception -> assertThat(((SignNoUnavailableException) exception).reason())
                        .isEqualTo(SignNoUnavailableReason.INVALID_HIGH_WATER_MARK));
    }

    @Test
    void shouldMapDatabaseFailureToUnavailable() {
        given(sequenceMapper.selectMaxSignNo("S20261023", "S20261024"))
                .willThrow(new DataAccessResourceFailureException("database down"));

        assertThatThrownBy(() -> highWaterMark.currentMaxSequence(BUSINESS_DATE))
                .isInstanceOf(SignNoUnavailableException.class)
                .satisfies(exception -> assertThat(((SignNoUnavailableException) exception).reason())
                        .isEqualTo(SignNoUnavailableReason.HIGH_WATER_MARK_UNAVAILABLE));
    }
}
