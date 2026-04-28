package com.backend.backend.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        System.out.println("🔥 JWT FILTER HIT: " + path);

        // ✅ ALWAYS SKIP AUTH ROUTES
        if (path.startsWith("/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            String token = extractToken(request);

            if (token != null) {

                DecodedJWT jwt = JWT.require(jwtConfig.algorithm())
                        .build()
                        .verify(token);

                String userId = jwt.getSubject();

                if (userId != null) {

                    UUID uuid = UUID.fromString(userId);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    uuid,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
                            );

                    SecurityContextHolder.getContext().setAuthentication(auth);
                    UserContext.setCurrentUser(uuid);
                }
            }

            filterChain.doFilter(request, response);
            System.out.println("TOKEN FOUND = " + token);

        } catch (Exception e) {
            System.out.println("❌ JWT ERROR: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } finally {
            UserContext.clear();
        }
    }

    // ------------------------------------------------------------
    // PUBLIC ROUTES (OAuth + static)
    // ------------------------------------------------------------
    private boolean isPublicRoute(String path) {
        return path.startsWith("/auth/")
                || path.startsWith("/oauth2/")
                || path.startsWith("/error")
                || path.startsWith("/favicon");
    }

    // ------------------------------------------------------------
    // TOKEN EXTRACTION
    // ------------------------------------------------------------
    private String extractToken(HttpServletRequest request) {

        System.out.println("🍪 COOKIES:");
        // 1. Cookie (production)
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 2. Authorization header (dev/testing)
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        return null;
    }
}