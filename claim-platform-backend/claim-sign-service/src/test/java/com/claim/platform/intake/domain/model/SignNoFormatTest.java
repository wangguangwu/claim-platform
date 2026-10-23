package com.claim.platform.intake.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

/**
 * 验证签收号格式、解析、上限和日期区间规则。
 *
 * @author wangguangwu
 */
class SignNoFormatTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 10, 23);

    @Test
    void shouldFormatDateAndLeftPaddedSequence() {
        assertThat(SignNoFormat.format(BUSINESS_DATE, 1)).isEqualTo("S2026102300000001");
        assertThat(SignNoFormat.format(BUSINESS_DATE, 42)).isEqualTo("S2026102300000042");
        assertThat(SignNoFormat.format(BUSINESS_DATE, 99_999_999L)).isEqualTo("S2026102399999999");
        assertThat(SignNoFormat.format(BUSINESS_DATE, 1)).hasSize(SignNoFormat.SIGN_NO_LENGTH);
    }

    @Test
    void shouldRejectNullDateAndOutOfRangeSequence() {
        assertThatThrownBy(() -> SignNoFormat.format(null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("业务日期");
        assertThatThrownBy(() -> SignNoFormat.format(BUSINESS_DATE, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("越界");
        assertThatThrownBy(() -> SignNoFormat.format(BUSINESS_DATE, SignNoFormat.MAX_SEQUENCE + 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("越界");
    }

    @Test
    void shouldRecognizeOnlyCurrentFormat() {
        assertThat(SignNoFormat.isCurrentFormat(SignNoFormat.format(BUSINESS_DATE, 1))).isTrue();
        assertThat(SignNoFormat.isCurrentFormat("SIGN001")).isFalse();
        assertThat(SignNoFormat.isCurrentFormat("S202610230000000")).isFalse();
        assertThat(SignNoFormat.isCurrentFormat("S20261023000000011")).isFalse();
        assertThat(SignNoFormat.isCurrentFormat("X2026102300000001")).isFalse();
        assertThat(SignNoFormat.isCurrentFormat("S20261023ABCDEFGH")).isFalse();
        assertThat(SignNoFormat.isCurrentFormat(null)).isFalse();
    }

    @Test
    void shouldParseDateAndSequence() {
        String signNo = SignNoFormat.format(BUSINESS_DATE, 123);

        assertThat(SignNoFormat.parseDate(signNo)).isEqualTo(BUSINESS_DATE);
        assertThat(SignNoFormat.parseSequence(signNo)).isEqualTo(123);
    }

    @Test
    void shouldRejectParsingHistoricalFormat() {
        assertThatThrownBy(() -> SignNoFormat.parseDate("SIGN001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("当前格式");
        assertThatThrownBy(() -> SignNoFormat.parseSequence("SIGN001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("当前格式");
    }

    @Test
    void shouldBuildHalfOpenDateRangeAcrossBoundaries() {
        assertThat(SignNoFormat.dateRangeStart(LocalDate.of(2026, 12, 31))).isEqualTo("S20261231");
        assertThat(SignNoFormat.dateRangeEnd(LocalDate.of(2026, 12, 31))).isEqualTo("S20270101");
        assertThat(SignNoFormat.dateRangeStart(BUSINESS_DATE)).isEqualTo("S20261023");
        assertThat(SignNoFormat.dateRangeEnd(BUSINESS_DATE)).isEqualTo("S20261024");
    }
}
