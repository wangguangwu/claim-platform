package com.claim.platform.sign.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

/**
 * 验证签收号格式、解析、上限和日期区间规则。
 *
 * @author wangguangwu
 */
class SignNoTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 10, 23);

    @Test
    void shouldFormatDateAndLeftPaddedSequence() {
        assertThat(SignNo.format(BUSINESS_DATE, 1)).isEqualTo("S2026102300000001");
        assertThat(SignNo.format(BUSINESS_DATE, 42)).isEqualTo("S2026102300000042");
        assertThat(SignNo.format(BUSINESS_DATE, 99_999_999L)).isEqualTo("S2026102399999999");
        assertThat(SignNo.format(BUSINESS_DATE, 1)).hasSize(SignNo.SIGN_NO_LENGTH);
    }

    @Test
    void shouldRejectNullDateAndOutOfRangeSequence() {
        assertThatThrownBy(() -> SignNo.format(null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("业务日期");
        assertThatThrownBy(() -> SignNo.format(BUSINESS_DATE, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("越界");
        assertThatThrownBy(() -> SignNo.format(BUSINESS_DATE, SignNo.MAX_SEQUENCE + 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("越界");
    }

    @Test
    void shouldRecognizeOnlyCurrentFormat() {
        assertThat(SignNo.isCurrentFormat(SignNo.format(BUSINESS_DATE, 1))).isTrue();
        assertThat(SignNo.isCurrentFormat("SIGN001")).isFalse();
        assertThat(SignNo.isCurrentFormat("S202610230000000")).isFalse();
        assertThat(SignNo.isCurrentFormat("S20261023000000011")).isFalse();
        assertThat(SignNo.isCurrentFormat("X2026102300000001")).isFalse();
        assertThat(SignNo.isCurrentFormat("S20261023ABCDEFGH")).isFalse();
        assertThat(SignNo.isCurrentFormat(null)).isFalse();
    }

    @Test
    void shouldParseDateAndSequence() {
        String signNo = SignNo.format(BUSINESS_DATE, 123);

        assertThat(SignNo.parseDate(signNo)).isEqualTo(BUSINESS_DATE);
        assertThat(SignNo.parseSequence(signNo)).isEqualTo(123);
    }

    @Test
    void shouldRejectParsingHistoricalFormat() {
        assertThatThrownBy(() -> SignNo.parseDate("SIGN001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("当前格式");
        assertThatThrownBy(() -> SignNo.parseSequence("SIGN001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("当前格式");
    }

    @Test
    void shouldBuildHalfOpenDateRangeAcrossBoundaries() {
        assertThat(SignNo.dateRangeStart(LocalDate.of(2026, 12, 31))).isEqualTo("S20261231");
        assertThat(SignNo.dateRangeEnd(LocalDate.of(2026, 12, 31))).isEqualTo("S20270101");
        assertThat(SignNo.dateRangeStart(BUSINESS_DATE)).isEqualTo("S20261023");
        assertThat(SignNo.dateRangeEnd(BUSINESS_DATE)).isEqualTo("S20261024");
    }
}
