package com.neuroguard.riskalertservice.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String secret;

    public DecodedJWT verifyToken(String token) {
        return JWT.require(Algorithm.HMAC256(secret)).build().verify(token);
    }

    public String getUsernameFromToken(String token) {
        return verifyToken(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return verifyToken(token).getClaim("role").asString();
    }

    public Long getUserIdFromToken(String token) {
        return verifyToken(token).getClaim("userId").asLong();
    }

    public boolean validateToken(String token) {
        try {
            verifyToken(token);
            return true;
        } catch (JWTVerificationException e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}