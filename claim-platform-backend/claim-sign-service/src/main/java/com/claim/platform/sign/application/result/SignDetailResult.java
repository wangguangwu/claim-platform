package com.claim.platform.sign.application.result;

import com.claim.platform.sign.domain.model.SignStatus;
import com.claim.platform.sign.domain.model.Material;

import java.util.List;

/**
 * 表示申请详情查询的对外数据，不返回内部版本号或幂等记录。
 *
 * @author wangguangwu
 */
public record SignDetailResult(String signNo, String externalApplyNo, String applicantName,
                                      SignStatus status, List<Material> materials) {
}
