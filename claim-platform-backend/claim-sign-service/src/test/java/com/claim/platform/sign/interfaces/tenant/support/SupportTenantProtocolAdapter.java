package com.claim.platform.sign.interfaces.tenant.support;

import com.claim.platform.sign.interfaces.tenant.core.converter.RequestConverter;
import com.claim.platform.sign.interfaces.tenant.core.parser.RequestParser;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolAdapter;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolKey;
import com.claim.platform.sign.interfaces.tenant.core.validator.RequestValidator;

/**
 * 测试租户的完整组件包。
 *
 * @author wangguangwu
 */
public record SupportTenantProtocolAdapter(TenantProtocolKey key,
                                           RequestParser<SupportTenantSignPayload> parser,
                                           RequestValidator<SupportTenantSignPayload> validator,
                                           RequestConverter<SupportTenantSignPayload> converter)
        implements TenantProtocolAdapter<SupportTenantSignPayload> {
}
