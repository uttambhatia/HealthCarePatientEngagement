package com.healthcare.gateway.utils;

import com.healthcare.platform.common.observability.CorrelationIdHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("X-Correlation-Id"))
                .orElse(UUID.randomUUID().toString());
        CorrelationIdHolder.set(correlationId);
        ServerHttpRequest request = exchange.getRequest().mutate().header("X-Correlation-Id", correlationId).build();
        exchange.getResponse().getHeaders().add("X-Correlation-Id", correlationId);
        return chain.filter(exchange.mutate().request(request).build())
                .doFinally(signalType -> CorrelationIdHolder.clear());
    }
}
