package com.claim.platform.sign.interfaces.tenant.reference;

import com.claim.platform.sign.interfaces.tenant.core.converter.RequestConverter;
import com.claim.platform.sign.interfaces.tenant.core.parser.DefaultJsonParser;
import com.claim.platform.sign.interfaces.tenant.core.parser.RequestParser;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolHandler;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolKey;
import com.claim.platform.sign.interfaces.tenant.core.validator.DefaultRequestValidator;
import com.claim.platform.sign.interfaces.tenant.core.validator.RequestValidator;
import com.claim.platform.sign.interfaces.tenant.reference.converter.ReferenceRequestConverter;
import com.claim.platform.sign.interfaces.tenant.reference.dto.ReferenceIntakePayload;
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
public class ReferenceProtocolHandler implements TenantProtocolHandler<ReferenceIntakePayload> {

    public static final String TENANT_CODE = "reference";
    public static final String PROTOCOL_VERSION = "v1";

    private final TenantProtocolKey key = new TenantProtocolKey(TENANT_CODE, PROTOCOL_VERSION);
    private final RequestParser<ReferenceIntakePayload> parser;
    private final RequestValidator<ReferenceIntakePayload> validator;
    private final RequestConverter<ReferenceIntakePayload> converter;

    public ReferenceProtocolHandler(ObjectMapper objectMapper, Validator validator) {
        this.parser = new DefaultJsonParser<>(objectMapper, ReferenceIntakePayload.class);
        this.validator = new ReferenceRequestValidator(new DefaultRequestValidator<>(validator));
        this.converter = new ReferenceRequestConverter(objectMapper);
    }

    @Override
    public TenantProtocolKey key() {
        return key;
    }

    @Override
    public RequestParser<ReferenceIntakePayload> parser() {
        return parser;
    }

    @Override
    public RequestValidator<ReferenceIntakePayload> validator() {
        return validator;
    }

    @Override
    public RequestConverter<ReferenceIntakePayload> converter() {
        return converter;
    }
}
