package com.claim.platform.intake.interfaces.tenant.support;

import com.claim.platform.intake.interfaces.tenant.core.context.TenantContext;

/**
 * 转换层测试使用的租户上下文和报文样例。
 *
 * @author wangguangwu
 */
public final class SupportTenantTestData {

    public static final String TENANT_CODE = "tenant-a";
    public static final String PROTOCOL_VERSION = "v1";
    public static final String TRACE_ID = "trace-001";
    public static final String CERTIFICATE_NO = "110101199001011234";
    public static final String MOBILE = "13800138000";

    public static final String VALID_PAYLOAD = """
            {
              "externalApplyNo": "EXT-001",
              "tenantCode": "forged-tenant",
              "protocolVersion": "v9",
              "accidentDate": "2026-10-01",
              "applicant": {
                "name": "张三",
                "certificateType": "IDCARD",
                "certificateNo": "%s",
                "mobile": "%s",
                "gender": "M",
                "birthDate": "1990-01-01",
                "relationshipToInsured": "SELF"
              },
              "extension": {
                "channel": "APP"
              }
            }
            """.formatted(CERTIFICATE_NO, MOBILE);

    private SupportTenantTestData() {
    }

    public static TenantContext context() {
        return new TenantContext(TENANT_CODE, PROTOCOL_VERSION, TRACE_ID);
    }

    public static String payloadWithCertificateType(String certificateType) {
        return VALID_PAYLOAD.replace("\"certificateType\": \"IDCARD\"",
                "\"certificateType\": \"" + certificateType + "\"");
    }

    public static String payloadWithoutExternalApplyNo() {
        return VALID_PAYLOAD.replace("\"externalApplyNo\": \"EXT-001\",", "");
    }

    public static String payloadWithoutRelationship() {
        return VALID_PAYLOAD.replace(",\n    \"relationshipToInsured\": \"SELF\"", "");
    }
}
