package com.claim.platform.intake.application.result;

import com.claim.platform.intake.domain.model.IntakeStatus;

/**
 * 表示写入命令可安全回传的签收申请结果。
 *
 * @author wangguangwu
 */
public record ClaimIntakeResult(String signNo, IntakeStatus status) {
}
