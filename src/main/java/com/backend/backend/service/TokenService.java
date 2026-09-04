package com.backend.backend.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.backend.backend.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


    @Service
    @RequiredArgsConstructor
    public class TokenService {

        private final JwtConfig jwtConfig;

        public DecodedJWT verify(String token) {
            return jwtConfig.verifier().verify(token);
        }

        public String getEmail(String token) {
            return verify(token).getClaim("email").asString();
        }

        public String getName(String token) {
            return verify(token).getClaim("name").asString();
        }

        public String getUserId(String token) {
            return verify(token).getSubject();
        }
    }
