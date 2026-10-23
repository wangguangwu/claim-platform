package com.claim.platform.intake.interfaces.tenant.reference.converter;

import com.claim.platform.intake.application.command.SubmitIntakeCommand;
import com.claim.platform.intake.domain.model.CertificateType;
import com.claim.platform.intake.domain.model.person.Applicant;
import com.claim.platform.intake.interfaces.tenant.core.context.TenantContext;
import com.claim.platform.intake.interfaces.tenant.core.converter.CodeMapper;
import com.claim.platform.intake.interfaces.tenant.core.converter.RequestConverter;
import com.claim.platform.intake.interfaces.tenant.reference.dto.ReferenceApplicantPayload;
import com.claim.platform.intake.interfaces.tenant.reference.dto.ReferenceIntakePayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.TreeMap;

/**
 * reference 参考协议的转换器：显式映射申请人字段、收敛证件编码，并只从协议声明字段构造扩展 JSON。
 *
 * @author wangguangwu
 */
public class ReferenceRequestConverter implements RequestConverter<ReferenceIntakePayload> {

    private static final CodeMapper<CertificateType> CERTIFICATE_TYPES = new CodeMapper<>(
            "applicant.certificateType",
            Map.of("IDCARD", CertificateType.ID_CARD, "PASSPORT", CertificateType.PASSPORT));

    private final ObjectMapper objectMapper;

    public ReferenceRequestConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SubmitIntakeCommand convert(ReferenceIntakePayload request, TenantContext context) {
        ReferenceApplicantPayload applicant = request.applicant();
        CertificateType certificateType = CERTIFICATE_TYPES.map(applicant.certificateType());
        Applicant domainApplicant = new Applicant(applicant.name(), certificateType.name(),
                applicant.certificateNo(), applicant.mobile(), applicant.gender(),
                applicant.birthDate(), applicant.relationshipToInsured());
        return new SubmitIntakeCommand(context.tenantCode(), context.traceId(), request.externalApplyNo(),
                applicant.name(), domainApplicant, null, null, null, extensionJson(request));
    }

    private String extensionJson(ReferenceIntakePayload request) {
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
