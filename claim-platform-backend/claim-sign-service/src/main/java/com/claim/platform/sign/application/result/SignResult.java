package com.claim.platform.sign.application.result;

import com.claim.platform.sign.domain.model.SignStatus;

/**
 * 表示写入命令可安全回传的签收申请结果。
 *
 * @author wangguangwu
 */
public record SignResult(String signNo, SignStatus status) {
}
