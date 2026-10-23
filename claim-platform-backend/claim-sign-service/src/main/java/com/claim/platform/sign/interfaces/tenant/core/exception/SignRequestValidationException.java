package com.claim.platform.sign.interfaces.tenant.core.exception;

/**
 * 表示租户 DTO 未通过公共校验或租户差异校验，命令转换不得执行。
 *
 * @author wangguangwu
 */
public class SignRequestValidationException extends SignConversionException {

    public SignRequestValidationException(String fieldPath) {
        super("请求校验失败", fieldPath);
    }
}
