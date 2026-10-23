package com.claim.platform.intake.domain.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 平台签收号格式规则：{@code S + yyyyMMdd + 8 位左补零流水号}。
 * 只承载纯格式与解析规则，不依赖 Redis、Spring 或数据库；
 * 历史 {@code SIGN} 编号不参与新格式解析。
 *
 * @author wangguangwu
 */
public final class SignNoFormat {

    public static final String PREFIX = "S";
    public static final int SEQUENCE_DIGITS = 8;
    public static final int SIGN_NO_LENGTH = 17;
    public static final long MAX_SEQUENCE = 99_999_999L;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Pattern CURRENT_FORMAT = Pattern.compile("^S\\d{" + (SEQUENCE_DIGITS + 8) + "}$");

    private SignNoFormat() {
    }

    /**
     * 按业务日期和流水号生成新格式签收号。
     *
     * @param businessDate 业务日期
     * @param sequence     当日流水号，范围 1..99,999,999
     * @return 固定 17 位签收号
     * @throws IllegalArgumentException 日期为空或流水号越界时抛出
     */
    public static String format(LocalDate businessDate, long sequence) {
        if (businessDate == null) {
            throw new IllegalArgumentException("业务日期不能为空");
        }
        if (sequence < 1 || sequence > MAX_SEQUENCE) {
            throw new IllegalArgumentException("签收号流水号越界");
        }
        return PREFIX + businessDate.format(DATE_FORMATTER)
                + String.format(Locale.ROOT, "%0" + SEQUENCE_DIGITS + "d", sequence);
    }

    /**
     * 判断编号是否符合当前新格式；历史 {@code SIGN} 编号返回 {@code false}。
     */
    public static boolean isCurrentFormat(String signNo) {
        return signNo != null && CURRENT_FORMAT.matcher(signNo).matches();
    }

    /**
     * 从新格式编号解析业务日期。
     *
     * @throws IllegalArgumentException 编号不是新格式时抛出
     */
    public static LocalDate parseDate(String signNo) {
        requireCurrentFormat(signNo);
        return LocalDate.parse(signNo.substring(1, 9), DATE_FORMATTER);
    }

    /**
     * 从新格式编号解析流水号。
     *
     * @throws IllegalArgumentException 编号不是新格式时抛出
     */
    public static long parseSequence(String signNo) {
        requireCurrentFormat(signNo);
        return Long.parseLong(signNo.substring(9, SIGN_NO_LENGTH));
    }

    /**
     * 返回指定业务日期高水位查询的闭开区间起点，例如 {@code S20261023}。
     */
    public static String dateRangeStart(LocalDate businessDate) {
        return PREFIX + businessDate.format(DATE_FORMATTER);
    }

    /**
     * 返回指定业务日期高水位查询的闭开区间终点，即次日的起点。
     */
    public static String dateRangeEnd(LocalDate businessDate) {
        return PREFIX + businessDate.plusDays(1).format(DATE_FORMATTER);
    }

    private static void requireCurrentFormat(String signNo) {
        if (!isCurrentFormat(signNo)) {
            throw new IllegalArgumentException("不是当前格式的签收号");
        }
    }
}
