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

        // ============================
        // DEBUG: REQUEST ENTRY
        // ============================
        System.out.println("\n==============================");
        System.out.println("➡️ REQUEST: " + request.getMethod() + " " + path);

        try {

            // ============================
            // Skip public routes
            // ============================
            if (isPublicRoute(path)) {
                System.out.println("🟢 Public route - skipping auth");
                filterChain.doFilter(request, response);
                return;
            }

            // ============================
            // Extract token
            // ============================
            String token = extractToken(request);

            if (token == null || token.isBlank()) {

                SecurityContextHolder.clearContext();
                UserContext.clear();

                System.out.println("❌ NO TOKEN FOUND");

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"TOKEN_MISSING\"}");
                return;
            }

            System.out.println("🔐 Token received");

            // ============================
            // Verify JWT
            // ============================
            DecodedJWT jwt;
            try {
                jwt = JWT.require(jwtConfig.algorithm())
                        .build()
                        .verify(token);
            } catch (Exception e) {
                System.out.println("🔴 JWT verification failed: " + e.getMessage());

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"INVALID_TOKEN\"}");
                return;
            }

            String subject = jwt.getSubject();

            if (subject == null || subject.isBlank()) {
                System.out.println("🔴 JWT missing subject");

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"INVALID_SUBJECT\"}");
                return;
            }

            UUID userId = UUID.fromString(subject);

            // ============================
            // Roles
            // ============================
            String accessRole = safe(jwt.getClaim("accessRole").asString(), "USER");
            String appRole = safe(jwt.getClaim("appRole").asString(), "MEMBER");

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            authorities.add(new SimpleGrantedAuthority("ROLE_" + accessRole.toUpperCase()));
            authorities.add(new SimpleGrantedAuthority("APP_" + appRole.toUpperCase()));

            // ============================
            // AUTH SUCCESS
            // ============================
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            authorities
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserContext.setCurrentUser(userId);

            // ============================
            // DEBUG SUCCESS BLOCK
            // ============================
            System.out.println("🟢 AUTH SUCCESS");
            System.out.println("UserId: " + userId);
            System.out.println("AccessRole: " + accessRole);
            System.out.println("AppRole: " + appRole);

            filterChain.doFilter(request, response);

        } catch (Exception ex) {

            System.out.println("💥 FILTER ERROR: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());


            if (!response.isCommitted()) {
                response.resetBuffer();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"AUTH_FILTER_ERROR\"}");
            }

            SecurityContextHolder.clearContext();
            UserContext.clear();

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"AUTH_FILTER_ERROR\"}");

        } finally {
            UserContext.clear();
            System.out.println("🏁 REQUEST END: " + path);
            System.out.println("==============================\n");
        }
    }

    // ============================
    // TOKEN EXTRACTION
    // ============================
    private String extractToken(HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    System.out.println("🍪 Token found in cookie");
                    return cookie.getValue();
                }
            }
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            System.out.println("📦 Token found in header");
            return authHeader.substring(7);
        }

        System.out.println("⚠️ No token found in request");
        return null;
    }

    // ============================
    // PUBLIC ROUTES
    // ============================
    private boolean isPublicRoute(String path) {
        return path.startsWith("/auth/")
                || path.equals("/")
                || path.equals("/error")
                || path.equals("/favicon.ico")
                || path.startsWith("/pricing")
                || path.startsWith("/privacy")
                || path.startsWith("/terms")
                || path.equals("/users/oauth-login")
                || path.equals("/users/demo-login");
    }

    // ============================
    // SAFE STRING
    // ============================
    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}