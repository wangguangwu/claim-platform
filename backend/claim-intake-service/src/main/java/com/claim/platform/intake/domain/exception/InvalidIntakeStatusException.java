package com.claim.platform.intake.domain.exception;

import com.claim.platform.intake.domain.model.IntakeStatus;

/**
 * 表示命令与当前签收申请状态机不兼容。
 *
 * @author wangguangwu
 */
public class InvalidIntakeStatusException extends RuntimeException {
    public InvalidIntakeStatusException(IntakeStatus status) { super("当前申请状态不允许该操作: " + status); }
}
