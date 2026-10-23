package com.claim.platform.sign.application.port;

import java.time.LocalDate;

/**
 * 查询 MySQL 主库中指定业务日期已使用的最大流水号。
 * 查询包含软删除行并排除历史 {@code SIGN} 编号，必须在创建事务之外执行。
 *
 * @author wangguangwu
 */
public interface SignNoHighWaterMark {

    /**
     * 返回指定业务日期已落库的最大流水号；没有记录时返回 0。
     *
     * @throws com.claim.platform.sign.application.exception.SignNoUnavailableException
     *         查询失败或高水位非法时抛出
     */
    long currentMaxSequence(LocalDate businessDate);
}
