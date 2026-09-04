package com.backend.backend.config;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer:the-manager-life}")
    private String issuer;

    @Value("${jwt.audience:the-manager-life-api}")
    private String audience;

    public Algorithm algorithm() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 characters");
        }
        return Algorithm.HMAC256(secret);
    }

    public JWTVerifier verifier() {
        return com.auth0.jwt.JWT.require(algorithm())
                .withIssuer(issuer)
                .withAudience(audience)
                .build();
    }
}
