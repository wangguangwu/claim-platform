package com.claim.platform.sign.application.exception;

/**
 * 表示同一幂等键的签收提交正在处理中，当前请求未能在等待时间内获取业务锁。
 *
 * @author wangguangwu
 */
public class SignSubmissionInProgressException extends RuntimeException {

    public SignSubmissionInProgressException() {
        super("申请正在处理中，请稍后重试");
    }
}
