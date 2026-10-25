package com.claim.platform.gateway.filter;

import com.claim.platform.common.trace.TraceContext;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Generates or forwards a trace identifier before any route filter and returns it to the caller.
 *
 * @author wangguangwu
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceGatewayFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String requestedTraceId = exchange.getRequest().getHeaders().getFirst(TraceContext.TRACE_ID_HEADER);
        String traceId = requestedTraceId == null || requestedTraceId.isBlank()
                ? java.util.UUID.randomUUID().toString().replace("-", "") : requestedTraceId;
        ServerWebExchange tracedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate().header(TraceContext.TRACE_ID_HEADER, traceId).build())
                .response(exchange.getResponse()).build();
        tracedExchange.getResponse().getHeaders().set(TraceContext.TRACE_ID_HEADER, traceId);
        return chain.filter(tracedExchange)
                .contextWrite(context -> context.put(TraceContext.TRACE_ID_MDC_KEY, traceId))
                .doOnEach(signal -> MDC.put(TraceContext.TRACE_ID_MDC_KEY, traceId))
                .doFinally(signal -> MDC.remove(TraceContext.TRACE_ID_MDC_KEY));
    }
}
