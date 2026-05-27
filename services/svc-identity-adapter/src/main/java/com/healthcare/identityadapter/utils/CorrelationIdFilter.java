package com.healthcare.identityadapter.utils;

import com.healthcare.platform.common.observability.CorrelationIdHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = Optional.ofNullable(request.getHeader("X-Correlation-Id")).orElse(UUID.randomUUID().toString());
        CorrelationIdHolder.set(correlationId);
        MDC.put("correlationId", correlationId);
        response.addHeader("X-Correlation-Id", correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
            CorrelationIdHolder.clear();
        }
    }
}
