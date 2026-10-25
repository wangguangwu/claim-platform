package com.claim.platform.sign.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.claim.platform.sign.infrastructure.persistence.dataobject.SignDO;
import org.apache.ibatis.annotations.Mapper;

/** Provides MyBatis-Plus access to Sign facts without exposing persistence types to the domain.
 *
 * @author wangguangwu
 */
@Mapper
public interface SignMapper extends BaseMapper<SignDO> {
}
