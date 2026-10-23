package com.claim.platform.sign.domain.exception;

import com.claim.platform.sign.domain.model.SignStatus;

/**
 * 表示命令与当前签收申请状态机不兼容。
 *
 * @author wangguangwu
 */
public class InvalidSignStatusException extends RuntimeException {
    public InvalidSignStatusException(SignStatus status) {
        super("当前申请状态不允许该操作: " + status);
    }
}
