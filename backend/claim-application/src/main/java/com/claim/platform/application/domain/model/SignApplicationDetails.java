package com.claim.platform.application.domain.model;

import java.util.List;

/**
 * 表示按租户隔离加载的签收申请及其材料查询事实。
 *
 * @author wangguangwu
 */
public record SignApplicationDetails(SignApplication application, List<SignMaterial> materials) {
}
