package com.neuroguard.consultationservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class KeycloakRequestAttributeFilter extends OncePerRequestFilter {

    private static final List<String> APP_ROLES = List.of("ADMIN", "PROVIDER", "PATIENT", "CAREGIVER");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            if (request.getAttribute("userId") == null) {
                request.setAttribute("userId", extractUserId(jwtAuth));
            }
            if (request.getAttribute("userRole") == null) {
                request.setAttribute("userRole", extractRole(jwtAuth));
            }
        }

        filterChain.doFilter(request, response);
    }

    private Long extractUserId(JwtAuthenticationToken jwtAuth) {
        Object userId = jwtAuth.getToken().getClaim("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        if (userId instanceof String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }

        String username = jwtAuth.getToken().getClaimAsString("preferred_username");
        return switch (username == null ? "" : username) {
            case "patient", "demo" -> 1L;
            case "provider" -> 2L;
            case "caregiver" -> 3L;
            case "admin" -> 4L;
            default -> 1L;
        };
    }

    @SuppressWarnings("unchecked")
    private String extractRole(JwtAuthenticationToken jwtAuth) {
        Map<String, Object> realmAccess = jwtAuth.getToken().getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            for (String role : APP_ROLES) {
                if (roles.contains(role)) {
                    return role;
                }
            }
        }
        return "PATIENT";
    }
}
