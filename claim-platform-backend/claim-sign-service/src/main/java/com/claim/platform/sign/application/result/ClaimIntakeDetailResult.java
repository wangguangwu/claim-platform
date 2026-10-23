package com.claim.platform.sign.application.result;

import com.claim.platform.sign.domain.model.IntakeStatus;
import com.claim.platform.sign.domain.model.IntakeMaterial;

import java.util.List;

/**
 * 表示申请详情查询的对外数据，不返回内部版本号或幂等记录。
 *
 * @author wangguangwu
 */
public record ClaimIntakeDetailResult(String signNo, String externalApplyNo, String applicantName,
                                      IntakeStatus status, List<IntakeMaterial> materials) {
}
