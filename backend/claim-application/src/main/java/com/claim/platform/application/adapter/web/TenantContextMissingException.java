package com.claim.platform.application.adapter.web;

/**
 * 表示受信任调用链未提供租户上下文，禁止回退到请求体或路径中的租户值。
 *
 * @author wangguangwu
 */
public class TenantContextMissingException extends RuntimeException {
    public TenantContextMissingException() { super("缺少租户上下文"); }
}
