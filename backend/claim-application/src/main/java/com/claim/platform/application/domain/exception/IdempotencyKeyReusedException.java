package com.claim.platform.application.domain.exception;

/**
 * 表示相同幂等键被用于语义不同的命令，防止误回放结果。
 *
 * @author wangguangwu
 */
public class IdempotencyKeyReusedException extends RuntimeException {
    public IdempotencyKeyReusedException() { super("幂等键已被用于不同请求"); }
}
