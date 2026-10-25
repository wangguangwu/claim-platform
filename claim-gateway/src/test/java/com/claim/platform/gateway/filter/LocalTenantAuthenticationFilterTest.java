package com.claim.platform.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.claim.platform.gateway.config.LocalTenantProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

/** Verifies that only configured local credentials create a trusted tenant header. @author wangguangwu */
class LocalTenantAuthenticationFilterTest {
    @Test
    void shouldReplaceCallerTenantHeaderWhenConfiguredCredentialIsValid() {
        LocalTenantProperties properties = properties();
        LocalTenantAuthenticationFilter filter = new LocalTenantAuthenticationFilter(properties, new ObjectMapper());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/signs")
                .header(LocalTenantAuthenticationFilter.LOCAL_CREDENTIAL_HEADER, "local-token")
                .header(LocalTenantAuthenticationFilter.TENANT_HEADER, "untrusted-tenant")
                .build());
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();

        filter.filter(exchange, forwarded -> {
            forwardedExchange.set(forwarded);
            return forwarded.getResponse().setComplete();
        }).block();

        assertThat(forwardedExchange.get().getRequest().getHeaders()
                .getFirst(LocalTenantAuthenticationFilter.TENANT_HEADER)).isEqualTo("tenant-a");
    }

    @Test
    void shouldRejectUnknownLocalCredentialWithSafeResponse() {
        LocalTenantAuthenticationFilter filter = new LocalTenantAuthenticationFilter(properties(), new ObjectMapper());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/signs")
                .header(LocalTenantAuthenticationFilter.LOCAL_CREDENTIAL_HEADER, "unknown")
                .build());

        filter.filter(exchange, forwarded -> forwarded.getResponse().setComplete()).block();

        MockServerHttpResponse response = exchange.getResponse();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBodyAsString().block()).contains("UNAUTHORIZED").doesNotContain("unknown");
    }

    private LocalTenantProperties properties() {
        LocalTenantProperties properties = new LocalTenantProperties();
        properties.setEnabled(true);
        properties.setCredentials(Map.of("local-token", "tenant-a"));
        return properties;
    }
}
