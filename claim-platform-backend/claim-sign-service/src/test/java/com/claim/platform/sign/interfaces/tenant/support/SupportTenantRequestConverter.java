package com.claim.platform.sign.interfaces.tenant.support;

import com.claim.platform.sign.application.command.SignCommand;
import com.claim.platform.sign.domain.model.CertificateType;
import com.claim.platform.sign.domain.model.person.Applicant;
import com.claim.platform.sign.interfaces.tenant.core.context.TenantContext;
import com.claim.platform.sign.interfaces.tenant.core.converter.CodeMapper;
import com.claim.platform.sign.interfaces.tenant.core.converter.RequestConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 测试租户转换器：演示字段显式映射、编码收敛和可信元数据填充。
 *
 * @author wangguangwu
 */
public class SupportTenantRequestConverter implements RequestConverter<SupportTenantSignPayload> {

    private static final CodeMapper<CertificateType> CERTIFICATE_TYPES = new CodeMapper<>(
            "applicant.certificateType",
            Map.of("IDCARD", CertificateType.ID_CARD, "PASSPORT", CertificateType.PASSPORT));

    private final ObjectMapper objectMapper;

    public SupportTenantRequestConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SignCommand convert(SupportTenantSignPayload request, TenantContext context) {
        SupportTenantApplicantPayload applicant = request.applicant();
        CertificateType certificateType = CERTIFICATE_TYPES.map(applicant.certificateType());
        Applicant domainApplicant = new Applicant(applicant.name(), certificateType.name(),
                applicant.certificateNo(), applicant.mobile(), applicant.gender(),
                applicant.birthDate(), applicant.relationshipToInsured());
        return new SignCommand(context.tenantCode(), context.traceId(), request.externalApplyNo(),
                applicant.name(), domainApplicant, null, null, null, List.of(), extensionJson(request));
    }

    private String extensionJson(SupportTenantSignPayload request) {
        Map<String, String> extension = new TreeMap<>();
        extension.put("accidentDate", request.accidentDate());
        if (request.extension() != null) {
            extension.putAll(request.extension());
        }
        try {
            return objectMapper.writeValueAsString(extension);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("扩展字段序列化失败", exception);
        }
    }
}
