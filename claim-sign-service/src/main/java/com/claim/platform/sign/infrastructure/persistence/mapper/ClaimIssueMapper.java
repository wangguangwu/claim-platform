package com.claim.platform.sign.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.claim.platform.sign.infrastructure.persistence.dataobject.ClaimIssueDO;
import org.apache.ibatis.annotations.Mapper;

/** Provides MyBatis-Plus access to ClaimIssue persistence rows.
 *
 * @author wangguangwu
 */
@Mapper
public interface ClaimIssueMapper extends BaseMapper<ClaimIssueDO> {
}
