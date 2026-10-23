package com.claim.platform.sign.interfaces.tenant.reference;

import static org.assertj.core.api.Assertions.assertThat;

import com.claim.platform.sign.interfaces.tenant.core.context.TenantContext;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolAdapterRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ReferenceProtocolAdapterRegistrationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(Validator.class, () -> Validation.buildDefaultValidatorFactory().getValidator())
            .withUserConfiguration(ReferenceProtocolAdapter.class, TenantProtocolAdapterRegistry.class);

    @Test
    void shouldRegisterReferenceProtocolInSpringContext() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            TenantProtocolAdapterRegistry registry = context.getBean(TenantProtocolAdapterRegistry.class);
            TenantContext tenantContext = new TenantContext(ReferenceProtocolAdapter.TENANT_CODE,
                    ReferenceProtocolAdapter.PROTOCOL_VERSION, "trace-001");

            assertThat(registry.get(tenantContext)).isInstanceOf(ReferenceProtocolAdapter.class);
        });
    }
}
