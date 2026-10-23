package com.claim.platform.sign.interfaces.tenant.core.converter;

import com.claim.platform.sign.application.command.SignCommand;
import com.claim.platform.sign.interfaces.tenant.core.context.TenantContext;

/**
 * 一次调用把租户 DTO 完整转换为平台标准签收命令。
 * 租户字段、材料编码和证件类型必须在本阶段收敛，不得把租户原始编码写入命令。
 *
 * @param <T> 当前租户的报文 DTO 类型
 * @author wangguangwu
 */
public interface RequestConverter<T> {

    /**
     * 转换租户 DTO。
     *
     * @param request 已通过校验的租户 DTO
     * @param context 可信租户上下文，只用于填充命令的可信元数据
     * @return 完整的平台标准签收命令
     */
    SignCommand convert(T request, TenantContext context);
}
