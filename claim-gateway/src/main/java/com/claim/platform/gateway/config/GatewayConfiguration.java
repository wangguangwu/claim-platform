package com.claim.platform.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Supplies gateway components that are independent of any business protocol.
 *
 * @author wangguangwu
 */
@Configuration
@EnableConfigurationProperties(LocalTenantProperties.class)
public class GatewayConfiguration {
    /** Groups rate-limit buckets by trusted tenant header, falling back to remote address. */
    @Bean
    public KeyResolver tenantKeyResolver() {
        return exchange -> Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("X-Tenant-Code"))
                .switchIfEmpty(Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                        .map(address -> address.getAddress().getHostAddress()))
                .defaultIfEmpty("anonymous");
    }
}
