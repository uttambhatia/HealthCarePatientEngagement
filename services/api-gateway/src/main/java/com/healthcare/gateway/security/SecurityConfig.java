package com.healthcare.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Configuration
public class SecurityConfig {
    private static final String[] HUMAN_ROLES = {"PATIENT", "DOCTOR", "COORDINATOR", "ADMIN"};
    private static final String[] CLINICAL_ROLES = {"DOCTOR", "COORDINATOR", "ADMIN"};

    @Bean
    @ConditionalOnProperty(prefix = "platform.security", name = "enabled", havingValue = "false")
    public SecurityWebFilterChain disabledSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ReactiveJwtDecoder jwtDecoder(
            @Value("${platform.security.oauth2.jwk-set-uri:https://login.microsoftonline.com/common/discovery/v2.0/keys}") String jwkSetUri,
            @Value("${platform.security.oauth2.issuer:}") String issuer,
            @Value("${platform.security.oauth2.audience:}") String audience) {
        if (issuer == null || issuer.isBlank() || audience == null || audience.isBlank()) {
            throw new IllegalStateException("platform.security.oauth2.issuer and platform.security.oauth2.audience are required when security is enabled");
        }
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withAudience = token -> {
            Object audClaim = token.getClaims().get("aud");

            boolean audienceMatched = token.getAudience().stream().anyMatch(audience::equals);
            if (!audienceMatched && audClaim instanceof String audAsString) {
                audienceMatched = audience.equals(audAsString);
            }
            if (!audienceMatched && audClaim instanceof Collection<?> audAsCollection) {
                audienceMatched = audAsCollection.stream().anyMatch(value -> audience.equals(String.valueOf(value)));
            }

            return audienceMatched
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "The required audience is missing", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
        return decoder;
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .pathMatchers("/bff/**").hasAnyRole(HUMAN_ROLES)

                        // Human-facing domain APIs.
                        .pathMatchers(HttpMethod.POST, "/api/patients", "/api/patients/", "/patients", "/patients/").permitAll()
                        .pathMatchers("/api/patients/**", "/patients/**").hasAnyRole(HUMAN_ROLES)
                        .pathMatchers("/api/consents", "/api/consents/**").hasAnyRole(HUMAN_ROLES)
                        .pathMatchers("/api/appointments", "/api/appointments/**").hasAnyRole(HUMAN_ROLES)
                        .pathMatchers("/api/careplans", "/api/careplans/**").hasAnyRole(HUMAN_ROLES)
                        .pathMatchers("/api/medical-records", "/api/medical-records/**").hasAnyRole(HUMAN_ROLES)
                        .pathMatchers("/api/notifications", "/api/notifications/**").hasAnyRole("PATIENT", "DOCTOR", "COORDINATOR", "ADMIN", "SYSTEM_INTEGRATION")
                        .pathMatchers("/api/telemetry", "/api/telemetry/**").hasAnyRole("PATIENT", "DOCTOR", "COORDINATOR", "ADMIN", "SYSTEM_INTEGRATION")
                        .pathMatchers("/api/alerts", "/api/alerts/**").hasAnyRole("PATIENT", "DOCTOR", "COORDINATOR", "ADMIN", "SYSTEM_INTEGRATION")

                        // Machine or operations APIs.
                        .pathMatchers("/api/devices/events", "/api/devices/events/**").hasAnyRole("SYSTEM_INTEGRATION", "DEVICE_IDENTITY", "ADMIN")
                        .pathMatchers("/api/identity/assertions", "/api/identity/assertions/**").hasAnyRole("SYSTEM_INTEGRATION", "ADMIN")
                        .pathMatchers("/api/servicebus/messages", "/api/servicebus/messages/**").hasAnyRole("SYSTEM_INTEGRATION", "COORDINATOR", "ADMIN")

                        // Fallback: authenticated + clinical/ops visibility only.
                        .pathMatchers("/api/**").hasAnyRole(CLINICAL_ROLES)
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("X-Correlation-Id"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
