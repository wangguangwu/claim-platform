package com.claim.platform.intake.interfaces.tenant.support;

import com.claim.platform.intake.interfaces.tenant.core.converter.RequestConverter;
import com.claim.platform.intake.interfaces.tenant.core.parser.RequestParser;
import com.claim.platform.intake.interfaces.tenant.core.registry.TenantProtocolHandler;
import com.claim.platform.intake.interfaces.tenant.core.registry.TenantProtocolKey;
import com.claim.platform.intake.interfaces.tenant.core.validator.RequestValidator;

/**
 * 测试租户的完整组件包。
 *
 * @author wangguangwu
 */
public record SupportTenantProtocolHandler(TenantProtocolKey key,
                                           RequestParser<SupportTenantIntakePayload> parser,
                                           RequestValidator<SupportTenantIntakePayload> validator,
                                           RequestConverter<SupportTenantIntakePayload> converter)
        implements TenantProtocolHandler<SupportTenantIntakePayload> {
}
