package com.neuroguard.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${app.security.keycloak-enabled:true}")
    private boolean keycloakEnabled;

    /**
     * JWK Set URI — used to validate token signatures.
     * In Docker: http://keycloak:8080/realms/neuroguard/protocol/openid-connect/certs
     * Locally:   http://localhost:8180/realms/neuroguard/protocol/openid-connect/certs
     */
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    // -------------------------------------------------------------------------
    // CORS
    // -------------------------------------------------------------------------

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",
            "http://127.0.0.1:4200",
            "http://localhost:80",
            "http://localhost",
            "http://frontend",
            "http://frontend:80"
        ));
        config.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(Arrays.asList(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.CONTENT_TYPE
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        return new CorsWebFilter(corsConfigurationSource());
    }

    // -------------------------------------------------------------------------
    // JWT Decoder — validates SIGNATURE only, skips issuer check.
    //
    // WHY: The token is issued by Keycloak at http://localhost:8180 (browser-visible URL)
    // but the gateway runs in Docker where Keycloak is at http://keycloak:8080.
    // The "iss" claim in the token says "localhost:8180" but the gateway's issuer-uri
    // env var is "keycloak:8080" → mismatch → 401.
    // Solution: decode via JWK public keys (cryptographically safe) and skip the issuer
    // string comparison, which is redundant when using asymmetric key validation.
    // -------------------------------------------------------------------------

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        // NimbusReactiveJwtDecoder validates the RS256 signature using Keycloak's public keys.
        // No issuer validator is added → the "iss" claim mismatch between localhost:8180
        // and keycloak:8080 no longer causes a 401.
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    // -------------------------------------------------------------------------
    // Security filter chain
    // -------------------------------------------------------------------------

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        if (keycloakEnabled) {
            http
                .authorizeExchange(exchange -> exchange
                    .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .pathMatchers(
                        "/actuator/**",
                        "/auth/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/webjars/**"
                    ).permitAll()
                    .anyExchange().authenticated()
                )
                // Use our custom decoder (signature-only validation, no issuer check)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder())));
        } else {
            http.authorizeExchange(exchange -> exchange.anyExchange().permitAll());
        }

        return http.build();
    }
}
