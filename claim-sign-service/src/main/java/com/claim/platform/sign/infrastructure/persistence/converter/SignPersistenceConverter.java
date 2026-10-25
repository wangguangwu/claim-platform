package com.claim.platform.sign.infrastructure.persistence.converter;

import com.claim.platform.sign.domain.sign.model.Sign;
import com.claim.platform.sign.domain.sign.model.SignNo;
import com.claim.platform.sign.domain.sign.model.SignStatus;
import com.claim.platform.sign.infrastructure.persistence.dataobject.SignDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps structural Sign fields for persistence while aggregate reconstruction retains domain invariants.
 *
 * @author wangguangwu
 */
@Mapper(componentModel = "spring")
public interface SignPersistenceConverter {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantCode", expression = "java(source.tenantCode())")
    @Mapping(target = "signNo", expression = "java(source.signNo().value())")
    @Mapping(target = "externalApplyNo", expression = "java(source.externalApplyNo())")
    @Mapping(target = "applicantName", expression = "java(source.applicantName())")
    @Mapping(target = "signStatus", expression = "java(source.status().name())")
    @Mapping(target = "protocolVersion", expression = "java(source.protocolVersion())")
    @Mapping(target = "version", constant = "0")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", constant = "0")
    SignDO toDataObject(Sign source);

    /** Reconstructs the aggregate through its validating constructor rather than bypassing invariants. */
    default Sign toDomain(SignDO source) {
        return new Sign(source.getTenantCode(), new SignNo(source.getSignNo()), source.getExternalApplyNo(),
                source.getApplicantName(), source.getProtocolVersion(), SignStatus.valueOf(source.getSignStatus()));
    }
}
