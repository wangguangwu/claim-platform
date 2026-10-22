package com.claim.platform.application.usecase.result;

import com.claim.platform.application.domain.model.SignApplicationStatus;

/**
 * 表示写入命令可安全回传的签收申请结果。
 *
 * @author wangguangwu
 */
public record SignApplicationResult(String signNo, SignApplicationStatus status) {
}
