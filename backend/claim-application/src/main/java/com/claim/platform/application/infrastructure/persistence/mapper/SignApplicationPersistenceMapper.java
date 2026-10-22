package com.claim.platform.application.infrastructure.persistence.mapper;

import com.claim.platform.application.domain.model.SignApplication;
import com.claim.platform.application.domain.model.SignApplicationState;
import com.claim.platform.application.domain.model.SignMaterial;
import com.claim.platform.application.infrastructure.persistence.SignApplicationDO;
import com.claim.platform.application.infrastructure.persistence.SignMaterialDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 在基础设施层转换 MyBatis-Plus 数据对象与领域状态；
 * Domain 不依赖 MapStruct、MyBatis-Plus 或数据库字段。
 *
 * @author wangguangwu
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SignApplicationPersistenceMapper {

    /**
     * 将申请 DO 转为不含数据库技术字段的领域状态。
     */
    SignApplicationState toState(SignApplicationDO source);

    /**
     * 将领域状态转为新申请写入使用的 DO，数据库主键和版本由持久化层管理。
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    SignApplicationDO toDO(SignApplicationState source);

    /**
     * 将材料 DO 映射为领域值对象，过滤租户、删除标记等存储技术字段。
     */
    SignMaterial toDomain(SignMaterialDO source);

    /**
     * 通过领域状态恢复聚合，状态转换仍由聚合自身控制。
     */
    default SignApplication toDomain(SignApplicationDO source) {
        return SignApplication.restore(toState(source));
    }

    /**
     * 导出聚合快照并映射为 DO，不将数据库版本回流到领域层。
     */
    default SignApplicationDO toDO(SignApplication source) {
        return toDO(source.snapshot());
    }
}
