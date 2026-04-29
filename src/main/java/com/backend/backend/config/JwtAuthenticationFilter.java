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

            // -------------------------------------------------
            // Skip public routes completely
            // -------------------------------------------------
            if (isPublicRoute(path)) {
                filterChain.doFilter(request, response);
                return;
            }

            // -------------------------------------------------
            // Already authenticated? continue
            // -------------------------------------------------
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // -------------------------------------------------
            // Extract token
            // -------------------------------------------------
            String token = extractToken(request);

            if (token != null && !token.isBlank()) {

                DecodedJWT jwt = JWT.require(jwtConfig.algorithm())
                        .build()
                        .verify(token);

                String subject = jwt.getSubject();

                if (subject != null && !subject.isBlank()) {

                    UUID userId = UUID.fromString(subject);

                    String accessRole = jwt.getClaim("accessRole").asString();

                    if (accessRole == null || accessRole.isBlank()) {
                        accessRole = "USER";
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId,
                                    null,
                                    List.of(
                                            new SimpleGrantedAuthority("ROLE_" + accessRole)
                                    )
                            );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);

                    UserContext.setCurrentUser(userId);
                }
            }

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
    // Public routes
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
    // Cookie first, then Authorization header
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