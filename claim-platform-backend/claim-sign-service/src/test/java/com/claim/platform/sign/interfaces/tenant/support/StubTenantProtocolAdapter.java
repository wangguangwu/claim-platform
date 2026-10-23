package com.claim.platform.sign.interfaces.tenant.support;

import com.claim.platform.sign.interfaces.tenant.core.converter.RequestConverter;
import com.claim.platform.sign.interfaces.tenant.core.parser.RequestParser;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolAdapter;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolKey;
import com.claim.platform.sign.interfaces.tenant.core.validator.RequestValidator;

/**
 * 可注入 Mockito 组件的通用测试组件包，用于验证 Pipeline 顺序和注册表行为。
 *
 * @param <T> 报文 DTO 类型
 * @author wangguangwu
 */
public record StubTenantProtocolAdapter<T>(TenantProtocolKey key, RequestParser<T> parser,
                                           RequestValidator<T> validator,
                                           RequestConverter<T> converter)
        implements TenantProtocolAdapter<T> {
}
