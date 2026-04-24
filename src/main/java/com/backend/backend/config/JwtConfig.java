package com.backend.backend.config;

import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    public Algorithm algorithm() {
        return Algorithm.HMAC256(secret);
    }
}
