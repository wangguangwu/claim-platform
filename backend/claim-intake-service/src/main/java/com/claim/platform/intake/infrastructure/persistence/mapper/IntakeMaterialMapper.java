package com.claim.platform.intake.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.claim.platform.intake.infrastructure.persistence.dataobject.IntakeMaterialDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提供签收材料数据对象的 MyBatis-Plus Mapper 能力。
 *
 * @author wangguangwu
 */
@Mapper
public interface IntakeMaterialMapper extends BaseMapper<IntakeMaterialDO> {
}
