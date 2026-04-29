package com.backend.backend.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
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

        try {

            if (isPublicRoute(path)) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = extractToken(request);

            if (token == null || token.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            DecodedJWT jwt = JWT.require(jwtConfig.algorithm())
                    .build()
                    .verify(token);

            String subject = jwt.getSubject();

            if (subject == null || subject.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            UUID userId = UUID.fromString(subject);

            // -------------------------------------------------
            // ROLE EXTRACTION (NEW STRUCTURE)
            // -------------------------------------------------
            String accessRole = safe(jwt.getClaim("accessRole").asString(), "USER");
            String appRole = safe(jwt.getClaim("appRole").asString(), "MEMBER");

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            // base auth
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

            // access control layer (admin, sradmin, user)
            authorities.add(new SimpleGrantedAuthority("ACCESS_" + accessRole));

            // app feature layer (member, manager, etc)
            authorities.add(new SimpleGrantedAuthority("APP_" + appRole));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            authorities
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserContext.setCurrentUser(userId);

            filterChain.doFilter(request, response);

        } catch (Exception ex) {

            SecurityContextHolder.clearContext();
            UserContext.clear();

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"INVALID_TOKEN\"}");
        } finally {
            UserContext.clear();
        }
    }

    // -------------------------------------------------
    // SAFE STRING HELPER
    // -------------------------------------------------
    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    // -------------------------------------------------
    // PUBLIC ROUTES
    // -------------------------------------------------
    private boolean isPublicRoute(String path) {
        return path.startsWith("/auth/")
                || path.equals("/favicon.ico")
                || path.equals("/error")
                || path.equals("/")
                || path.startsWith("/pricing")
                || path.startsWith("/privacy")
                || path.startsWith("/terms")
                || path.equals("/users/oauth-login")
                || path.equals("/users/demo-login");
    }

    // -------------------------------------------------
    // COOKIE + HEADER TOKEN SUPPORT
    // -------------------------------------------------
    private String extractToken(HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }
}