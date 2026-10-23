package com.claim.platform.sign.interfaces.tenant.core.registry;

import com.claim.platform.sign.interfaces.tenant.core.converter.RequestConverter;
import com.claim.platform.sign.interfaces.tenant.core.parser.RequestParser;
import com.claim.platform.sign.interfaces.tenant.core.validator.RequestValidator;

/**
 * 一个租户协议版本的完整组件包，只绑定协议键和 Parser、Validator、Converter，不承担流程决策。
 *
 * @param <T> 当前租户的报文 DTO 类型
 * @author wangguangwu
 */
public interface TenantProtocolAdapter<T> {

    /**
     * 协议匹配键。
     *
     * @return 不可变协议键
     */
    TenantProtocolKey key();

    /**
     * 报文解析组件。
     *
     * @return 当前租户的解析器
     */
    RequestParser<T> parser();

    /**
     * 公共与租户差异校验组件。
     *
     * @return 当前租户的校验器
     */
    RequestValidator<T> validator();

    /**
     * 命令转换组件。
     *
     * @return 当前租户的转换器
     */
    RequestConverter<T> converter();
}
