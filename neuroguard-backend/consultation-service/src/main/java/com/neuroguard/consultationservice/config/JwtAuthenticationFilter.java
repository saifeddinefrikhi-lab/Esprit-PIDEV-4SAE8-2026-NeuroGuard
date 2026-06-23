package com.neuroguard.consultationservice.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JwtAuthenticationFilter
 *
 * Supports two token types:
 * - HS256 (local JWT issued by user-service): validated with HMAC secret
 * - RS256 (Keycloak JWT): signature already validated by the API Gateway.
 *   We trust it here and extract claims directly without re-verifying.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> APP_ROLES = List.of("ADMIN", "PROVIDER", "PATIENT", "CAREGIVER");

    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            DecodedJWT decoded = JWT.decode(token);
            String algorithm = decoded.getAlgorithm(); // "HS256" or "RS256"

            if ("RS256".equals(algorithm)) {
                // --- Keycloak token path ---
                // Gateway already validated the RS256 signature — trust it here.
                String role = extractKeycloakRole(decoded);
                String username = decoded.getClaim("preferred_username").asString();
                if (username == null || username.isEmpty()) {
                    username = decoded.getSubject();
                }

                Long userId = extractUserId(decoded, username);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                request.setAttribute("userId", userId);
                request.setAttribute("userRole", role);
                SecurityContextHolder.getContext().setAuthentication(authToken);

            } else {
                // --- Local HS256 JWT path ---
                if (!jwtUtils.validateJwtToken(token)) {
                    chain.doFilter(request, response);
                    return;
                }

                DecodedJWT decodedJWT = jwtUtils.verifyToken(token);
                String username = decodedJWT.getSubject();
                String role = decodedJWT.getClaim("role").asString();
                Long userId = decodedJWT.getClaim("userId").asLong();

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                request.setAttribute("userId", userId);
                request.setAttribute("userRole", role);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        } catch (Exception e) {
            logger.error("JWT filter error: " + e.getMessage());
        }

        chain.doFilter(request, response);
    }

    private String extractKeycloakRole(DecodedJWT decoded) {
        // 1. Custom "roles" root claim (set by neuroguard-profile scope mapper)
        try {
            List<String> roles = decoded.getClaim("roles").asList(String.class);
            if (roles != null) {
                for (String appRole : APP_ROLES) {
                    if (roles.stream().anyMatch(r -> r.equalsIgnoreCase(appRole))) {
                        return appRole;
                    }
                }
            }
        } catch (Exception ignored) {}

        // 2. Standard realm_access.roles (added by our realm-access-mapper)
        try {
            // realm_access is a JSON object — try string matching as fallback
            String raw = decoded.getClaim("realm_access").toString();
            for (String appRole : APP_ROLES) {
                if (raw.contains(appRole)) return appRole;
            }
        } catch (Exception ignored) {}

        return "PATIENT";
    }

    private Long extractUserId(DecodedJWT decoded, String username) {
        try {
            Object uid = decoded.getClaim("userId").as(Object.class);
            if (uid instanceof Number n) return n.longValue();
            if (uid instanceof String s) return Long.parseLong(s);
        } catch (Exception ignored) {}
        return 0L;
    }
}
