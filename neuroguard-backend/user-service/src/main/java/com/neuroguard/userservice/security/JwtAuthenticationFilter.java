package com.neuroguard.userservice.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.neuroguard.userservice.entities.User;
import com.neuroguard.userservice.entities.UserStatus;
import com.neuroguard.userservice.repositories.UserRepository;
import com.neuroguard.userservice.services.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    @Autowired
    private UserService userService;

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
            // Decode without verification to inspect the algorithm
            DecodedJWT decoded = JWT.decode(token);
            String algorithm = decoded.getAlgorithm(); // "HS256" = local, "RS256" = Keycloak

            if ("RS256".equals(algorithm)) {
                // --- Keycloak token path ---
                // The gateway already validated the RS256 signature — trust it here.
                // Extract role from the custom "roles" claim (set by neuroguard-profile scope mapper)
                // or fall back to realm_access.roles
                String role = extractKeycloakRole(decoded);
                String username = decoded.getSubject(); // Keycloak sub (UUID or preferred_username)

                // Try to get preferred_username claim (more readable)
                String preferredUsername = decoded.getClaim("preferred_username").asString();
                if (preferredUsername != null && !preferredUsername.isEmpty()) {
                    username = preferredUsername;
                }

                // Extract userId from custom claim
                Long userId = null;
                try {
                    userId = decoded.getClaim("userId").asLong();
                } catch (Exception ignored) {}

                // Check user status in DB if we can find the user
                if (username != null) {
                    Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(username);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        if (user.getStatus() == UserStatus.BANNED || user.getStatus() == UserStatus.DISABLED) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Account is banned or disabled\"}");
                            return;
                        }
                        // Use DB userId as source of truth
                        userId = user.getId();
                    }
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                request.setAttribute("userId", userId);
                request.setAttribute("userRole", role);
                SecurityContextHolder.getContext().setAuthentication(authToken);

            } else {
                // --- Local JWT (HS256) path --- original logic
                if (jwtUtils.isTokenInvalidated(token) || !jwtUtils.validateJwtToken(token)) {
                    chain.doFilter(request, response);
                    return;
                }

                DecodedJWT decodedJWT = jwtUtils.verifyToken(token);
                String username = decodedJWT.getSubject();
                String role = decodedJWT.getClaim("role").asString();
                Long userId = decodedJWT.getClaim("userId").asLong();
                Long tokenVersion = decodedJWT.getClaim("tokenVersion").asLong();

                Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(username);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    if (user.getStatus() == UserStatus.BANNED || user.getStatus() == UserStatus.DISABLED) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Account is banned or disabled\"}");
                        return;
                    }
                    if (tokenVersion == null || tokenVersion < user.getTokenVersion()) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Session invalidated. Please log in again.\"}");
                        return;
                    }
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                request.setAttribute("userId", userId);
                request.setAttribute("userRole", role);
                SecurityContextHolder.getContext().setAuthentication(authToken);

                if (userService != null) {
                    userService.updateLastSeen(username);
                }
            }

        } catch (Exception e) {
            logger.error("JWT authentication failed: " + e.getMessage());
            chain.doFilter(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Extracts the app role from a Keycloak token.
     * Checks: root "roles" claim (custom mapper), then realm_access.roles
     */
    private String extractKeycloakRole(DecodedJWT decoded) {
        // 1. Custom mapper puts roles in root "roles" claim
        try {
            List<String> roles = decoded.getClaim("roles").asList(String.class);
            if (roles != null) {
                for (String r : roles) {
                    String upper = r.toUpperCase();
                    if (upper.equals("ADMIN") || upper.equals("PROVIDER") ||
                        upper.equals("PATIENT") || upper.equals("CAREGIVER")) {
                        return upper;
                    }
                }
            }
        } catch (Exception ignored) {}

        // 2. Standard realm_access.roles
        try {
            com.auth0.jwt.interfaces.Claim realmAccess = decoded.getClaim("realm_access");
            if (!realmAccess.isNull()) {
                // realm_access is a JSON object — parse it
                String realmAccessJson = realmAccess.toString();
                if (realmAccessJson.contains("ADMIN")) return "ADMIN";
                if (realmAccessJson.contains("PROVIDER")) return "PROVIDER";
                if (realmAccessJson.contains("PATIENT")) return "PATIENT";
                if (realmAccessJson.contains("CAREGIVER")) return "CAREGIVER";
            }
        } catch (Exception ignored) {}

        return "PATIENT"; // safe default
    }
}
