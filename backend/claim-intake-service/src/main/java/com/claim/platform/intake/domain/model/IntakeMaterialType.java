package com.claim.platform.intake.domain.model;

/**
 * 平台标准受理材料类型；租户协议中的私有编码必须先映射为本枚举，再进入应用命令。
 *
 * @author wangguangwu
 */
public enum IntakeMaterialType {

    ID_CARD,
    MEDICAL_INVOICE,
    DIAGNOSIS_CERTIFICATE
}
