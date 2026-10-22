package com.claim.platform.application.domain.exception;

/**
 * 表示同一租户的外部申请编号已被签收，阻止重复业务事实。
 *
 * @author wangguangwu
 */
public class SignApplicationAlreadyExistsException extends RuntimeException {
    public SignApplicationAlreadyExistsException() { super("签收申请已存在"); }
}
