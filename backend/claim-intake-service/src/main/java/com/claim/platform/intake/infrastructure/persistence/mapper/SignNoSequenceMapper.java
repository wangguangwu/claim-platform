package com.claim.platform.intake.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 查询 Claim Intake 自有表中指定业务日期的签收号高水位。
 * 范围查询使用 uk_sign_no 索引，包含软删除行，并通过固定长度排除历史 {@code SIGN} 编号。
 *
 * @author wangguangwu
 */
@Mapper
public interface SignNoSequenceMapper {

    /**
     * 返回闭开区间内固定 17 位签收号的最大值；没有记录时返回 null。
     * 调用方必须使用方法参数 {@code start/end} 限制业务日期。
     */
    @Select("""
            SELECT MAX(sign_no)
            FROM t_claim_intake
            WHERE sign_no >= #{start}
              AND sign_no < #{end}
              AND CHAR_LENGTH(sign_no) = 17
            """)
    String selectMaxSignNo(@Param("start") String start, @Param("end") String end);
}
