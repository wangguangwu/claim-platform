package com.claim.platform.sign.interfaces.tenant.reference;

/**
 * 定义 reference 参考协议的字段路径与固定编码。
 * 真实租户不得复用这些常量，必须定义自己的协议键和编码映射。
 *
 * @author wangguangwu
 */
public final class ReferenceProtocolConstants {

    public static final String CERTIFICATE_TYPE_FIELD = "applicant.certificateType";
    public static final String CERTIFICATE_CODE_ID_CARD = "IDCARD";
    public static final String CERTIFICATE_CODE_PASSPORT = "PASSPORT";
    public static final String RELATIONSHIP_TO_INSURED_FIELD = "applicant.relationshipToInsured";
    public static final String EXTENSION_ACCIDENT_DATE = "accidentDate";

    private ReferenceProtocolConstants() {
    }
}
