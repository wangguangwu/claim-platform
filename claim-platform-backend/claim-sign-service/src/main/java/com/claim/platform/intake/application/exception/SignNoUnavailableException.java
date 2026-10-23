package com.claim.platform.intake.application.exception;

/**
 * 平台暂时无法分配签收号；对外映射为 HTTP 503 和 {@code INTAKE_NUMBER_UNAVAILABLE}。
 * 该异常不代表申请已创建，调用方可以使用相同幂等键退避重试。
 *
 * @author wangguangwu
 */
public class SignNoUnavailableException extends RuntimeException {

    private final SignNoUnavailableReason reason;

    public SignNoUnavailableException(SignNoUnavailableReason reason) {
        super("签收号暂时不可用: " + reason);
        this.reason = reason;
    }

    public SignNoUnavailableException(SignNoUnavailableReason reason, Throwable cause) {
        super("签收号暂时不可用: " + reason, cause);
        this.reason = reason;
    }

    public SignNoUnavailableReason reason() {
        return reason;
    }
}
