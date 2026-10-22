package com.claim.platform.intake.infrastructure.persistence.converter;

import com.claim.platform.intake.domain.model.person.AccidentPerson;
import com.claim.platform.intake.domain.model.person.Applicant;
import com.claim.platform.intake.domain.model.person.MainInsured;
import com.claim.platform.intake.domain.model.person.Payee;
import com.claim.platform.intake.infrastructure.persistence.dataobject.person.AccidentPersonInfoDO;
import com.claim.platform.intake.infrastructure.persistence.dataobject.person.ApplicantInfoDO;
import com.claim.platform.intake.infrastructure.persistence.dataobject.person.MainInsuredInfoDO;
import com.claim.platform.intake.infrastructure.persistence.dataobject.person.PayeeInfoDO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * 在基础设施层双向转换四类人员持久化对象与领域 Person 子类型。
 * 字段命名在两侧一致，MapStruct 自动完成同名映射；
 * {@code unmappedTargetPolicy = ERROR} 强制转换完整性，新增字段遗漏将编译失败。
 *
 * @author wangguangwu
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PersonConverter {

    /**
     * 将申请人持久化对象转为领域申请人子类型。
     *
     * @param source 申请人 JSON 反序列化结果，可为 null
     * @return 领域申请人；源为 null 时返回 null
     */
    Applicant toApplicant(ApplicantInfoDO source);

    /**
     * 将出险人持久化对象转为领域出险人子类型。
     *
     * @param source 出险人 JSON 反序列化结果，可为 null
     * @return 领域出险人；源为 null 时返回 null
     */
    AccidentPerson toAccidentPerson(AccidentPersonInfoDO source);

    /**
     * 将主被保险人持久化对象转为领域主被保险人子类型。
     *
     * @param source 主被保险人 JSON 反序列化结果，可为 null
     * @return 领域主被保险人；源为 null 时返回 null
     */
    MainInsured toMainInsured(MainInsuredInfoDO source);

    /**
     * 将领款人持久化对象转为领域领款人子类型。
     *
     * @param source 领款人 JSON 反序列化结果，可为 null
     * @return 领域领款人；源为 null 时返回 null
     */
    Payee toPayee(PayeeInfoDO source);

    /**
     * 将领域申请人转为持久化对象用于写回 JSON 列。
     *
     * @param source 领域申请人，可为 null
     * @return 申请人持久化对象；源为 null 时返回 null
     */
    ApplicantInfoDO toApplicantInfoDO(Applicant source);

    /**
     * 将领域出险人转为持久化对象用于写回 JSON 列。
     *
     * @param source 领域出险人，可为 null
     * @return 出险人持久化对象；源为 null 时返回 null
     */
    AccidentPersonInfoDO toAccidentPersonInfoDO(AccidentPerson source);

    /**
     * 将领域主被保险人转为持久化对象用于写回 JSON 列。
     *
     * @param source 领域主被保险人，可为 null
     * @return 主被保险人持久化对象；源为 null 时返回 null
     */
    MainInsuredInfoDO toMainInsuredInfoDO(MainInsured source);

    /**
     * 将领域领款人转为持久化对象用于写回 JSON 列。
     *
     * @param source 领域领款人，可为 null
     * @return 领款人持久化对象；源为 null 时返回 null
     */
    PayeeInfoDO toPayeeInfoDO(Payee source);
}
