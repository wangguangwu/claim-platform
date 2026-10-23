package com.claim.platform.sign.application.exception;

/**
 * 当日签收号流水号达到 99,999,999 上限；对外映射为 HTTP 503 和 {@code INTAKE_NUMBER_EXHAUSTED}。
 * 系统不得截断、回绕或生成 9 位流水号。
 *
 * @author wangguangwu
 */
public class SignNoSequenceExhaustedException extends RuntimeException {

    public SignNoSequenceExhaustedException() {
        super("当日签收号流水号已用尽");
    }
}
