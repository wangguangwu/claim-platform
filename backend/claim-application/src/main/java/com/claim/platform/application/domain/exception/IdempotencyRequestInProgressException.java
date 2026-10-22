package com.claim.platform.application.domain.exception;

/**
 * 表示首个相同幂等命令仍在处理，调用方应以原键延迟重试。
 *
 * @author wangguangwu
 */
public class IdempotencyRequestInProgressException extends RuntimeException {
    public IdempotencyRequestInProgressException() { super("相同幂等请求正在处理中"); }
}
