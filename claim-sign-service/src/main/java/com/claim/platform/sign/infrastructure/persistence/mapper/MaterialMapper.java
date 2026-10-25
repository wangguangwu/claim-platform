package com.claim.platform.sign.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.claim.platform.sign.infrastructure.persistence.dataobject.MaterialDO;
import org.apache.ibatis.annotations.Mapper;

/** MyBatis-Plus access to material references. @author wangguangwu */
@Mapper public interface MaterialMapper extends BaseMapper<MaterialDO> { }
