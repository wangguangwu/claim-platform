package com.claim.platform.application.domain.exception;

import com.claim.platform.application.domain.model.SignApplicationStatus;

/**
 * 表示命令与当前签收申请状态机不兼容。
 *
 * @author wangguangwu
 */
public class InvalidApplicationStatusException extends RuntimeException {
    public InvalidApplicationStatusException(SignApplicationStatus status) { super("当前申请状态不允许该操作: " + status); }
}
