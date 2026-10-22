package com.claim.platform.intake.infrastructure.persistence.mapper;

import com.claim.platform.intake.domain.model.ClaimIntake;
import com.claim.platform.intake.domain.model.ClaimIntakeState;
import com.claim.platform.intake.domain.model.IntakeMaterial;
import com.claim.platform.intake.infrastructure.persistence.PersonConverter;
import com.claim.platform.intake.infrastructure.persistence.ClaimIntakeDO;
import com.claim.platform.intake.infrastructure.persistence.IntakeMaterialDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 在基础设施层转换 MyBatis-Plus 数据对象与领域状态；
 * 通过 {@code uses = PersonConverter.class} 在嵌套字段上完成四类人员 JSON 对象与领域 Person 子类型的双向转换。
 * Domain 不依赖 MapStruct、MyBatis-Plus 或数据库字段。
 *
 * @author wangguangwu
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = PersonConverter.class)
public interface ClaimIntakePersistenceMapper {

    /**
     * 将申请 DO 转为不含数据库技术字段的领域状态（含强类型 Person 子类型）。
     * 四个人员字段在 DO 侧为 InfoDO、状态侧为 Person 子类型，名称不同需显式映射，
     * 实际转换由 {@code uses = PersonConverter} 提供的双向方法完成。
     */
    @Mapping(source = "applicantInfo", target = "applicant")
    @Mapping(source = "accidentPersonInfo", target = "accidentPerson")
    @Mapping(source = "mainInsuredInfo", target = "mainInsured")
    @Mapping(source = "payeeInfo", target = "payee")
    ClaimIntakeState toState(ClaimIntakeDO source);

    /**
     * 将领域状态转为新申请写入使用的 DO，数据库主键和版本由持久化层管理。
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(source = "applicant", target = "applicantInfo")
    @Mapping(source = "accidentPerson", target = "accidentPersonInfo")
    @Mapping(source = "mainInsured", target = "mainInsuredInfo")
    @Mapping(source = "payee", target = "payeeInfo")
    ClaimIntakeDO toDO(ClaimIntakeState source);

    /**
     * 将材料 DO 映射为领域值对象，过滤租户、删除标记等存储技术字段。
     */
    IntakeMaterial toDomain(IntakeMaterialDO source);

    /**
     * 通过领域状态恢复聚合，状态转换仍由聚合自身控制。
     */
    default ClaimIntake toDomain(ClaimIntakeDO source) {
        return ClaimIntake.restore(toState(source));
    }

    /**
     * 导出聚合快照并映射为 DO，不将数据库版本回流到领域层。
     */
    default ClaimIntakeDO toDO(ClaimIntake source) {
        return toDO(source.snapshot());
    }
}
