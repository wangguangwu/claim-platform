package com.claim.platform.sign.interfaces.tenant.reference;

import com.claim.platform.sign.interfaces.tenant.core.converter.RequestConverter;
import com.claim.platform.sign.interfaces.tenant.core.parser.DefaultJsonParser;
import com.claim.platform.sign.interfaces.tenant.core.parser.RequestParser;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolAdapter;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolKey;
import com.claim.platform.sign.interfaces.tenant.core.validator.DefaultRequestValidator;
import com.claim.platform.sign.interfaces.tenant.core.validator.RequestValidator;
import com.claim.platform.sign.interfaces.tenant.reference.converter.ReferenceRequestConverter;
import com.claim.platform.sign.interfaces.tenant.reference.dto.ReferenceSignPayload;
import com.claim.platform.sign.interfaces.tenant.reference.validator.ReferenceRequestValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

/**
 * reference 参考协议的完整组件包，仅用于打通入口和验证扩展机制，不代表任何真实租户。
 * 真实租户必须新增独立组件包和协议键，不得复用本类的 {@code reference/v1}。
 *
 * @author wangguangwu
 */
@Component
public class ReferenceProtocolAdapter implements TenantProtocolAdapter<ReferenceSignPayload> {

    public static final String TENANT_CODE = "reference";
    public static final String PROTOCOL_VERSION = "v1";

    private final TenantProtocolKey key = new TenantProtocolKey(TENANT_CODE, PROTOCOL_VERSION);
    private final RequestParser<ReferenceSignPayload> parser;
    private final RequestValidator<ReferenceSignPayload> validator;
    private final RequestConverter<ReferenceSignPayload> converter;

    public ReferenceProtocolAdapter(ObjectMapper objectMapper, Validator validator) {
        this.parser = new DefaultJsonParser<>(objectMapper, ReferenceSignPayload.class);
        this.validator = new ReferenceRequestValidator(new DefaultRequestValidator<>(validator));
        this.converter = new ReferenceRequestConverter(objectMapper);
    }

    @Override
    public TenantProtocolKey key() {
        return key;
    }

    @Override
    public RequestParser<ReferenceSignPayload> parser() {
        return parser;
    }

    @Override
    public RequestValidator<ReferenceSignPayload> validator() {
        return validator;
    }

    @Override
    public RequestConverter<ReferenceSignPayload> converter() {
        return converter;
    }
}
