package com.claim.platform.application.usecase.result;

import com.claim.platform.application.domain.model.SignApplicationStatus;
import com.claim.platform.application.domain.model.SignMaterial;
import java.util.List;

/**
 * 表示申请详情查询的对外数据，不返回内部版本号或幂等记录。
 *
 * @author wangguangwu
 */
public record SignApplicationDetailResult(String signNo, String externalApplyNo, String applicantName,
                                          SignApplicationStatus status, List<SignMaterial> materials) {
}
