package com.backend.backend.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * Routes that NEVER require JWT
     * (OAuth + public endpoints)
     */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/",
            "/auth/google/",
            "/auth/google/login",
            "/auth/google/callback",
            "/users/auth/",
            "/error"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        try {

            // --------------------------------------------------
            // 1. PUBLIC ROUTES (skip EVERYTHING)
            // --------------------------------------------------
            if (path.startsWith("/auth/")) {
                filterChain.doFilter(request, response);
                return;
            }

            // --------------------------------------------------
            // 2. NO AUTH HEADER → just continue or reject depending route
            // --------------------------------------------------
            String header = request.getHeader("Authorization");

            if (header == null || !header.startsWith("Bearer ")) {

                // IMPORTANT:
                // do NOT auto-401 here for ALL routes
                filterChain.doFilter(request, response);
                return;
            }

            // --------------------------------------------------
            // 3. VALIDATE JWT ONLY WHEN PRESENT
            // --------------------------------------------------
            String token = header.substring(7);

            String userId = JWT.require(Algorithm.HMAC256(secret))
                    .build()
                    .verify(token)
                    .getSubject();

            UserContext.setCurrentUser(UUID.fromString(userId));

            filterChain.doFilter(request, response);

        } catch (Exception ex) {

            // DO NOT blindly block OAuth or public routes
            System.out.println("JWT ERROR: " + ex.getMessage());

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        } finally {

            UserContext.clear();
        }
    }

    /**
     * Centralized public route check
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
}