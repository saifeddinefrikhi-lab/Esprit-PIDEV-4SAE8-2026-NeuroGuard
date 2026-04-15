package com.neuroguard.userservice.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import com.neuroguard.userservice.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

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

        // Check if token has been invalidated (logged out)
        if (jwtUtils.isTokenInvalidated(token)) {
            chain.doFilter(request, response);
            return;
        }

        if (!jwtUtils.validateJwtToken(token)) {
            chain.doFilter(request, response);
            return;
        }

        // Extract claims
        DecodedJWT decodedJWT = jwtUtils.verifyToken(token);
        String username = decodedJWT.getSubject();
        String role = decodedJWT.getClaim("role").asString();
        Long userId = decodedJWT.getClaim("userId").asLong();

        // Create authentication token
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // Store userId in request attribute if needed
        request.setAttribute("userId", userId);
        request.setAttribute("userRole", role);

        SecurityContextHolder.getContext().setAuthentication(authToken);

        // Update last seen status
        if (userService != null) {
            userService.updateLastSeen(username);
        }

        chain.doFilter(request, response);
    }

}