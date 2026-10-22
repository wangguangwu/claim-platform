package com.claim.platform.application.domain.exception;

/**
 * 表示当前租户范围内不存在指定签收申请，避免暴露其他租户事实。
 *
 * @author wangguangwu
 */
public class SignApplicationNotFoundException extends RuntimeException {
    public SignApplicationNotFoundException() {
        super("签收申请不存在");
    }
}
