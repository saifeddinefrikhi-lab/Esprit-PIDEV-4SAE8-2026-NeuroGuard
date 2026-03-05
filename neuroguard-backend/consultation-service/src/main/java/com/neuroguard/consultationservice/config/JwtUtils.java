package com.neuroguard.consultationservice.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secretKey;

    private final Set<String> invalidatedTokens = ConcurrentHashMap.newKeySet();

    public DecodedJWT verifyToken(String token) {
        return JWT.require(Algorithm.HMAC256(secretKey)).build().verify(token);
    }

    public boolean validateJwtToken(String token) {
        try {
            verifyToken(token);
            return !isTokenInvalidated(token);
        } catch (Exception e) {
            return false;
        }
    }

    public void invalidateToken(String token) {
        invalidatedTokens.add(token);
    }

    public boolean isTokenInvalidated(String token) {
        return invalidatedTokens.contains(token);
    }
}