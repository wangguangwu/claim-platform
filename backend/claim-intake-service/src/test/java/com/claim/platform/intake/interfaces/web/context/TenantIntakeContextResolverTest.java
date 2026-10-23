package com.claim.platform.intake.interfaces.web.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.claim.platform.intake.interfaces.tenant.core.context.TenantContext;
import com.claim.platform.intake.interfaces.web.exception.TenantContextMissingException;
import com.claim.platform.intake.interfaces.web.exception.UntrustedCallerException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class TenantIntakeContextResolverTest {

    private static final String SERVICE_TOKEN = "service-token";

    private final TenantIntakeContextResolver resolver = new TenantIntakeContextResolver(
            new GatewayRequestContextResolver(SERVICE_TOKEN));

    @Test
    void shouldResolveTrustedContext() {
        MockHttpServletRequest request = request(SERVICE_TOKEN, "reference", "v1", "trace-001");

        TenantContext context = resolver.resolve(request);

        assertThat(context).isEqualTo(new TenantContext("reference", "v1", "trace-001"));
        assertThat(request.getAttribute(GatewayRequestContextResolver.TRACE_ATTRIBUTE)).isEqualTo("trace-001");
    }

    @Test
    void shouldGenerateTraceIdWhenTraceHeaderMissing() {
        MockHttpServletRequest request = request(SERVICE_TOKEN, "reference", "v1", null);

        TenantContext context = resolver.resolve(request);

        assertThat(context.traceId()).isNotBlank();
        assertThat(request.getAttribute(GatewayRequestContextResolver.TRACE_ATTRIBUTE))
                .isEqualTo(context.traceId());
    }

    @Test
    void shouldRejectMissingProtocolVersion() {
        MockHttpServletRequest request = request(SERVICE_TOKEN, "reference", null, "trace-001");

        assertThatThrownBy(() -> resolver.resolve(request)).isInstanceOf(TenantContextMissingException.class);
    }

    @Test
    void shouldRejectInvalidProtocolVersion() {
        MockHttpServletRequest request = request(SERVICE_TOKEN, "reference", "v 1", "trace-001");

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("协议版本");
    }

    @Test
    void shouldRejectUntrustedCaller() {
        MockHttpServletRequest request = request("wrong-token", "reference", "v1", "trace-001");

        assertThatThrownBy(() -> resolver.resolve(request)).isInstanceOf(UntrustedCallerException.class);
    }

    @Test
    void shouldRejectMissingTenant() {
        MockHttpServletRequest request = request(SERVICE_TOKEN, null, "v1", "trace-001");

        assertThatThrownBy(() -> resolver.resolve(request)).isInstanceOf(TenantContextMissingException.class);
    }

    private MockHttpServletRequest request(String serviceToken, String tenantCode, String protocolVersion,
                                           String traceId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (serviceToken != null) {
            request.addHeader("X-Claim-Service-Token", serviceToken);
        }
        if (tenantCode != null) {
            request.addHeader("X-Claim-Tenant", tenantCode);
        }
        if (protocolVersion != null) {
            request.addHeader(TenantIntakeContextResolver.PROTOCOL_VERSION_HEADER, protocolVersion);
        }
        if (traceId != null) {
            request.addHeader("X-Trace-Id", traceId);
        }
        return request;
    }
}
