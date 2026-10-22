package com.claim.platform.intake.interfaces.web.exception;

/**
 * 表示请求未携带受信任的服务间调用凭证，不能接受其租户头。
 *
 * @author wangguangwu
 */
public class UntrustedCallerException extends RuntimeException {
    public UntrustedCallerException() { super("调用方未通过内部鉴别"); }
}
