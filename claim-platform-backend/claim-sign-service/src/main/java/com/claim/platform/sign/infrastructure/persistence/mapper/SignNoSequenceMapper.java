package com.claim.platform.sign.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.claim.platform.sign.domain.model.SignNo;
import com.claim.platform.sign.infrastructure.persistence.SignTable;
import com.claim.platform.sign.infrastructure.persistence.dataobject.SignDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 查询理赔签收自有表中指定业务日期的签收号高水位。
 * 范围查询使用 uk_sign_no 索引，包含软删除行，并通过固定长度排除历史 {@code SIGN} 编号。
 *
 * @author wangguangwu
 */
@Mapper
public interface SignNoSequenceMapper extends BaseMapper<SignDO> {

    String MAX_SIGN_NO_EXPRESSION =
            "MAX(" + SignTable.SIGN_NO + ") AS " + SignTable.SIGN_NO;
    String SIGN_NO_LENGTH_EXPRESSION =
            "CHAR_LENGTH(" + SignTable.SIGN_NO + ") = {0}";

    /**
     * 返回闭开区间内固定 17 位签收号的最大值；没有记录时返回 null。
     * 调用方必须使用方法参数 {@code start/end} 限制业务日期。
     */
    default String selectMaxSignNo(String start, String end) {
        QueryWrapper<SignDO> query = new QueryWrapper<>();
        query.select(MAX_SIGN_NO_EXPRESSION)
                .ge(SignTable.SIGN_NO, start)
                .lt(SignTable.SIGN_NO, end)
                .apply(SIGN_NO_LENGTH_EXPRESSION, SignNo.SIGN_NO_LENGTH);
        SignDO result = selectOne(query);
        return result == null ? null : result.getSignNo();
    }
}
