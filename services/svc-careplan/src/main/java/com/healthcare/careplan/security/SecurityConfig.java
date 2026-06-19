package com.healthcare.careplan.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.http.HttpMethod;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
        @Bean
        @ConditionalOnProperty(prefix = "platform.security", name = "enabled", havingValue = "false", matchIfMissing = true)
        public SecurityFilterChain disabledSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        @Bean
        @ConditionalOnProperty(prefix = "platform.security", name = "enabled", havingValue = "true")
        public JwtDecoder jwtDecoder(
                @org.springframework.beans.factory.annotation.Value("${platform.security.oauth2.jwk-set-uri:https://login.microsoftonline.com/common/discovery/v2.0/keys}") String jwkSetUri,
                @org.springframework.beans.factory.annotation.Value("${platform.security.oauth2.issuer:}") String issuer,
                @org.springframework.beans.factory.annotation.Value("${platform.security.oauth2.audience:}") String audience) {
            if (issuer == null || issuer.isBlank() || audience == null || audience.isBlank()) {
                throw new IllegalStateException("platform.security.oauth2.issuer and platform.security.oauth2.audience are required when security is enabled");
            }
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
            OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
            OAuth2TokenValidator<Jwt> withAudience = token -> token.getAudience().stream().anyMatch(audience::equals)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "The required audience is missing", null));
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
            return decoder;
        }

    @Bean
        @ConditionalOnProperty(prefix = "platform.security", name = "enabled", havingValue = "true")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/careplans", "/careplans/**").hasAnyRole("PATIENT", "DOCTOR", "COORDINATOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/careplans").hasAnyRole("DOCTOR", "COORDINATOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/careplans/**").hasAnyRole("DOCTOR", "COORDINATOR", "ADMIN")
                .requestMatchers("/careplans/**").hasAnyRole("COORDINATOR", "ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
