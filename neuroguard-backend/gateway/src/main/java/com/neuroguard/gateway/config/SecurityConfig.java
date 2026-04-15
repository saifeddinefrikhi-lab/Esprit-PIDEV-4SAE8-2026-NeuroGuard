package com.neuroguard.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Gateway Security Configuration
 * The gateway acts as a reverse proxy and should allow requests through.
 * Authentication and authorization are handled by individual microservices.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) throws Exception {
        http
                // Disable CSRF for API endpoints
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // Disable Spring Security CORS here; gateway global CORS config handles it
                .cors(ServerHttpSecurity.CorsSpec::disable)
                // Disable HTTP Basic auth (JWT is used instead)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                // Disable form login
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                // All requests are permitted at gateway level
                // Microservices handle their own authentication/authorization
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()
                );

        return http.build();
    }
}
