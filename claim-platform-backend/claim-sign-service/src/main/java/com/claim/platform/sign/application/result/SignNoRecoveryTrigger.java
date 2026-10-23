package com.claim.platform.sign.application.result;

/**
 * 签收号恢复或校准的触发来源，用于稳定指标标签和日志。
 *
 * @author wangguangwu
 */
public enum SignNoRecoveryTrigger {

    MISSING_COUNTER("missing_counter"),
    INVALID_COUNTER("invalid_counter"),
    UNIQUE_CONFLICT("unique_conflict"),
    STARTUP("startup"),
    PERIODIC("periodic"),
    MANUAL("manual");

    private final String code;

    SignNoRecoveryTrigger(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
