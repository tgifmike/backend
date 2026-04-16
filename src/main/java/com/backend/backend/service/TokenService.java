package com.backend.backend.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Service;


    @Service
    public class TokenService {

        public DecodedJWT decode(String token) {
            return JWT.decode(token);
        }

        public String getEmail(String token) {
            return decode(token).getClaim("email").asString();
        }

        public String getName(String token) {
            return decode(token).getClaim("name").asString();
        }

        public String getUserId(String token) {
            return decode(token).getSubject();
        }
    }
