package com.claim.platform.intake.interfaces.tenant.core.validator;

/**
 * 校验租户 DTO 的公共规则与租户差异规则，不修改 DTO，也不执行模型转换。
 *
 * @param <T> 当前租户的报文 DTO 类型
 * @author wangguangwu
 */
public interface RequestValidator<T> {

    /**
     * 校验租户 DTO。
     *
     * @param request 已解析的租户 DTO
     */
    void validate(T request);
}
