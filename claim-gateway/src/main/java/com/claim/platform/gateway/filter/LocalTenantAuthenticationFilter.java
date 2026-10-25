package com.claim.platform.gateway.filter;

import com.claim.platform.common.api.ApiResult;
import com.claim.platform.common.trace.TraceContext;
import com.claim.platform.gateway.config.LocalTenantProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Maps an explicitly configured local-only test credential to a trusted tenant header at the edge.
 * No request body, path, query parameter or client tenant header is trusted.
 *
 * @author wangguangwu
 */
@Component
@Profile("local")
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class LocalTenantAuthenticationFilter implements GlobalFilter {
    static final String LOCAL_CREDENTIAL_HEADER = "X-Local-Test-Credential";
    static final String TENANT_HEADER = "X-Tenant-Code";
    private final LocalTenantProperties properties;
    private final ObjectMapper objectMapper;

    public LocalTenantAuthenticationFilter(LocalTenantProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String credential = exchange.getRequest().getHeaders().getFirst(LOCAL_CREDENTIAL_HEADER);
        if (credential == null) {
            return chain.filter(exchange);
        }
        String tenantCode = properties.getCredentials().get(credential);
        if (!properties.isEnabled() || tenantCode == null) {
            return writeUnauthorized(exchange);
        }
        ServerWebExchange authenticatedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .headers(headers -> { headers.remove(TENANT_HEADER); headers.set(TENANT_HEADER, tenantCode); })
                        .build())
                .build();
        return chain.filter(authenticatedExchange);
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange) {
        String traceId = exchange.getResponse().getHeaders().getFirst(TraceContext.TRACE_ID_HEADER);
        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(ApiResult.failure("UNAUTHORIZED", "调用方身份无效", traceId));
        } catch (JsonProcessingException exception) {
            payload = "{\"success\":false,\"code\":\"UNAUTHORIZED\"}".getBytes(StandardCharsets.UTF_8);
        }
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(payload)));
    }
}
