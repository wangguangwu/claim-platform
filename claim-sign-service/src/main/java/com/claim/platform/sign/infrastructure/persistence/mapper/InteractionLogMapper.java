package com.claim.platform.sign.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.claim.platform.sign.infrastructure.persistence.dataobject.InteractionLogDO;
import org.apache.ibatis.annotations.Mapper;

/** Provides MyBatis-Plus access to append-only interaction audit facts.
 *
 * @author wangguangwu
 */
@Mapper
public interface InteractionLogMapper extends BaseMapper<InteractionLogDO> {
}
