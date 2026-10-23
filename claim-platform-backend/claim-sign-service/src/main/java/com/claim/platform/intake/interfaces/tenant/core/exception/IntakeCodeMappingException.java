package com.claim.platform.intake.interfaces.tenant.core.exception;

/**
 * 表示租户编码在平台标准映射中不存在，禁止把租户原始编码透传到应用或领域层。
 *
 * @author wangguangwu
 */
public class IntakeCodeMappingException extends IntakeConversionException {

    public IntakeCodeMappingException(String fieldPath) {
        super("编码映射失败", fieldPath);
    }
}
