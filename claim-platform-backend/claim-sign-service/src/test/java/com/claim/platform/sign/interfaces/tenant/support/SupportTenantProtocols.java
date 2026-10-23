package com.claim.platform.sign.interfaces.tenant.support;

import com.claim.platform.sign.interfaces.tenant.core.parser.DefaultJsonParser;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolAdapter;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolAdapterRegistry;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolKey;
import com.claim.platform.sign.interfaces.tenant.core.validator.DefaultRequestValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.List;

/**
 * 测试租户组件工厂，只为验证扩展机制，不放入生产包。
 *
 * @author wangguangwu
 */
public final class SupportTenantProtocols {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private SupportTenantProtocols() {
    }

    public static SupportTenantProtocolAdapter handler(ObjectMapper objectMapper, TenantProtocolKey key) {
        return new SupportTenantProtocolAdapter(key,
                new DefaultJsonParser<>(objectMapper, SupportTenantSignPayload.class),
                new SupportTenantRequestValidator(new DefaultRequestValidator<>(VALIDATOR)),
                new SupportTenantRequestConverter(objectMapper));
    }

    public static SupportTenantProtocolAdapter v1(ObjectMapper objectMapper) {
        return handler(objectMapper, new TenantProtocolKey(
                SupportTenantTestData.TENANT_CODE, SupportTenantTestData.PROTOCOL_VERSION));
    }

    public static TenantProtocolAdapterRegistry registry(TenantProtocolAdapter<?>... handlers) {
        return new TenantProtocolAdapterRegistry(List.of(handlers));
    }
}
