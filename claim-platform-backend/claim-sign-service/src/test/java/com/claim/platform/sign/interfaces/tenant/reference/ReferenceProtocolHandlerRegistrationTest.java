package com.claim.platform.sign.interfaces.tenant.reference;

import static org.assertj.core.api.Assertions.assertThat;

import com.claim.platform.sign.interfaces.tenant.core.context.TenantContext;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolHandlerRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ReferenceProtocolHandlerRegistrationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(Validator.class, () -> Validation.buildDefaultValidatorFactory().getValidator())
            .withUserConfiguration(ReferenceProtocolHandler.class, TenantProtocolHandlerRegistry.class);

    @Test
    void shouldRegisterReferenceProtocolInSpringContext() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            TenantProtocolHandlerRegistry registry = context.getBean(TenantProtocolHandlerRegistry.class);
            TenantContext tenantContext = new TenantContext(ReferenceProtocolHandler.TENANT_CODE,
                    ReferenceProtocolHandler.PROTOCOL_VERSION, "trace-001");

            assertThat(registry.get(tenantContext)).isInstanceOf(ReferenceProtocolHandler.class);
        });
    }
}
