package com.claim.platform.sign.infrastructure.persistence.converter;

import com.claim.platform.sign.domain.model.Sign;
import com.claim.platform.sign.domain.model.SignState;
import com.claim.platform.sign.domain.model.Material;
import com.claim.platform.sign.infrastructure.persistence.dataobject.SignDO;
import com.claim.platform.sign.infrastructure.persistence.dataobject.MaterialDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 在基础设施层转换 MyBatis-Plus 数据对象与领域状态。
 * Domain 不依赖 MapStruct、MyBatis-Plus 或数据库字段。
 *
 * @author wangguangwu
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SignPersistenceConverter {

    /**
     * 将申请 DO 转为不含数据库技术字段的领域状态。
     */
    SignState toState(SignDO source);

    /**
     * 将领域状态转为新申请写入使用的 DO，数据库主键和版本由持久化层管理。
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    SignDO toDO(SignState source);

    /**
     * 将材料 DO 映射为领域值对象，过滤租户、删除标记等存储技术字段。
     */
    Material toDomain(MaterialDO source);

    /**
     * 通过领域状态恢复聚合，状态转换仍由聚合自身控制。
     */
    default Sign toDomain(SignDO source) {
        return Sign.restore(toState(source));
    }

    /**
     * 导出聚合快照并映射为 DO，不将数据库版本回流到领域层。
     */
    default SignDO toDO(Sign source) {
        return toDO(source.snapshot());
    }
}
