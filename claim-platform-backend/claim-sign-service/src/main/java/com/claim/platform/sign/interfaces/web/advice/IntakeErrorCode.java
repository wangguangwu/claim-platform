package com.claim.platform.sign.interfaces.web.advice;

import org.springframework.http.HttpStatus;

/**
 * 定义 Claim Intake 边界错误的稳定响应契约，集中错误码、HTTP 状态、文案和可选重试提示。
 *
 * @author wangguangwu
 */
public enum IntakeErrorCode {

    REQUEST_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "REQUEST_VALIDATION_FAILED", "请求参数不合法"),
    UNTRUSTED_CALLER(HttpStatus.UNAUTHORIZED, "UNTRUSTED_CALLER", "调用方未通过内部鉴别"),
    TENANT_CONTEXT_MISSING(HttpStatus.BAD_REQUEST, "TENANT_CONTEXT_MISSING", "缺少租户上下文"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "请求报文格式不合法"),
    TENANT_PROTOCOL_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "TENANT_PROTOCOL_NOT_SUPPORTED", "租户协议不支持"),
    CLAIM_INTAKE_NOT_FOUND(HttpStatus.NOT_FOUND, "CLAIM_INTAKE_NOT_FOUND", "签收申请不存在"),
    INVALID_INTAKE_STATUS(HttpStatus.CONFLICT, "INVALID_INTAKE_STATUS", "当前申请状态不允许该操作"),
    INTAKE_SUBMISSION_IN_PROGRESS(HttpStatus.CONFLICT, "INTAKE_SUBMISSION_IN_PROGRESS",
            "申请正在处理中，请稍后重试"),
    CLAIM_INTAKE_CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "CLAIM_INTAKE_CONCURRENT_MODIFICATION",
            "签收申请已被并发修改"),
    INTAKE_NUMBER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "INTAKE_NUMBER_UNAVAILABLE",
            "签收号服务暂不可用，请稍后重试", 5),
    INTAKE_NUMBER_EXHAUSTED(HttpStatus.SERVICE_UNAVAILABLE, "INTAKE_NUMBER_EXHAUSTED",
            "今日签收号已用尽，请联系平台"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "系统繁忙，请稍后重试");

    private final HttpStatus status;
    private final String code;
    private final String message;
    private final Integer retryAfterSeconds;

    IntakeErrorCode(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    IntakeErrorCode(HttpStatus status, String code, String message, Integer retryAfterSeconds) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public Integer retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
