package com.backend.backend.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtConfig jwtConfig;

    public JwtAuthenticationFilter(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())
                || SecurityContextHolder.getContext().getAuthentication() != null
                || isPublicRoute(request.getServletPath())) {
            chain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);
        if (token == null) {
            sendUnauthorized(response, "TOKEN_MISSING");
            return;
        }

        UUID userId;
        UsernamePasswordAuthenticationToken authentication;
        try {
            DecodedJWT jwt = jwtConfig.verifier().verify(token);
            userId = UUID.fromString(jwt.getSubject());
            String accessRole = safe(jwt.getClaim("accessRole").asString(), "USER");
            String appRole = safe(jwt.getClaim("appRole").asString(), "MEMBER");
            authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(
                            new SimpleGrantedAuthority("ROLE_" + accessRole.toUpperCase()),
                            new SimpleGrantedAuthority("APP_" + appRole.toUpperCase())
                    )
            );
        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
            UserContext.clear();
            sendUnauthorized(response, "INVALID_TOKEN");
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserContext.setCurrentUser(userId);
        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private static boolean isPublicRoute(String path) {
        return path.startsWith("/auth/")
                || path.equals("/error")
                || path.equals("/favicon.ico")
                || path.equals("/users/oauth-login")
                || path.equals("/users/demo-login")
                || path.startsWith("/api/email/")
                || path.equals("/api/s3/test");
    }

    private static void sendUnauthorized(HttpServletResponse response, String code) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + code + "\"}");
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
