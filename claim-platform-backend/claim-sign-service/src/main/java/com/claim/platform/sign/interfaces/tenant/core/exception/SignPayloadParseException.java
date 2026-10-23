package com.claim.platform.sign.interfaces.tenant.core.exception;

/**
 * 表示 Raw Payload 不符合当前租户报文结构，转换流程必须在此终止。
 *
 * @author wangguangwu
 */
public class SignPayloadParseException extends SignConversionException {

    public SignPayloadParseException(String fieldPath) {
        super("报文解析失败", fieldPath);
    }

    public SignPayloadParseException(String fieldPath, Throwable cause) {
        super("报文解析失败", fieldPath, cause);
    }
}
