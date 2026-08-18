//package com.backend.backend.config;
//
//import com.auth0.jwt.JWT;
//import com.auth0.jwt.interfaces.DecodedJWT;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.Cookie;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.http.HttpHeaders;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//
//@Component
//public class JwtAuthenticationFilter extends OncePerRequestFilter {
//
//    private final JwtConfig jwtConfig;
//
//    public JwtAuthenticationFilter(JwtConfig jwtConfig) {
//        this.jwtConfig = jwtConfig;
//    }
//
//    @Override
//    protected void doFilterInternal(
//            HttpServletRequest request,
//            HttpServletResponse response,
//            FilterChain filterChain
//    ) throws ServletException, IOException {
//
//        String path = request.getServletPath();
//
//        // ============================
//        // DEBUG: REQUEST ENTRY
//        // ============================
//        System.out.println("\n==============================");
//        System.out.println("➡️ REQUEST: " + request.getMethod() + " " + path);
//
//        try {
//
//            // ============================
//            // Skip public routes
//            // ============================
//            if (isPublicRoute(path)) {
//                System.out.println("🟢 Public route - skipping auth");
//                filterChain.doFilter(request, response);
//                return;
//            }
//
//            // ============================
//            // Extract token
//            // ============================
//            String token = extractToken(request);
//
//            if (token == null || token.isBlank()) {
//
//                SecurityContextHolder.clearContext();
//                UserContext.clear();
//
//                System.out.println("❌ NO TOKEN FOUND");
//
//                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                response.setContentType("application/json");
//                response.getWriter().write("{\"error\":\"TOKEN_MISSING\"}");
//                return;
//            }
//
//            System.out.println("🔐 Token received");
//
//            // ============================
//            // Verify JWT
//            // ============================
//            DecodedJWT jwt;
//            try {
//                jwt = JWT.require(jwtConfig.algorithm())
//                        .build()
//                        .verify(token);
//            } catch (Exception e) {
//                System.out.println("🔴 JWT verification failed: " + e.getMessage());
//
//                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                response.setContentType("application/json");
//                response.getWriter().write("{\"error\":\"INVALID_TOKEN\"}");
//                return;
//            }
//
//            String subject = jwt.getSubject();
//
//            if (subject == null || subject.isBlank()) {
//                System.out.println("🔴 JWT missing subject");
//
//                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                response.setContentType("application/json");
//                response.getWriter().write("{\"error\":\"INVALID_SUBJECT\"}");
//                return;
//            }
//
//            UUID userId = UUID.fromString(subject);
//
//            // ============================
//            // Roles
//            // ============================
//            String accessRole = safe(jwt.getClaim("accessRole").asString(), "USER");
//            String appRole = safe(jwt.getClaim("appRole").asString(), "MEMBER");
//
//            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
//
//            authorities.add(new SimpleGrantedAuthority("ROLE_" + accessRole.toUpperCase()));
//            authorities.add(new SimpleGrantedAuthority("APP_" + appRole.toUpperCase()));
//
//            // ============================
//            // AUTH SUCCESS
//            // ============================
//            UsernamePasswordAuthenticationToken authentication =
//                    new UsernamePasswordAuthenticationToken(
//                            userId,
//                            null,
//                            authorities
//                    );
//
//            SecurityContextHolder.getContext().setAuthentication(authentication);
//            UserContext.setCurrentUser(userId);
//
//            // ============================
//            // DEBUG SUCCESS BLOCK
//            // ============================
//            System.out.println("🟢 AUTH SUCCESS");
//            System.out.println("UserId: " + userId);
//            System.out.println("AccessRole: " + accessRole);
//            System.out.println("AppRole: " + appRole);
//
//            System.out.println("HOST HEADER = " + request.getHeader("host"));
//            System.out.println("ORIGIN HEADER = " + request.getHeader("origin"));
//            System.out.println("X-FORWARDED-HOST = " + request.getHeader("x-forwarded-host"));
//
//            filterChain.doFilter(request, response);
//
//        } catch (Exception ex) {
//
//            System.out.println("💥 FILTER ERROR: " + ex.getClass().getSimpleName());
//            ex.printStackTrace();
//
//            SecurityContextHolder.clearContext();
//            UserContext.clear();
//
//            if (!response.isCommitted()) {
//                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                response.setContentType("application/json");
//                response.getWriter().write("{\"error\":\"AUTH_FILTER_ERROR\"}");
//            }
//
//
//        } finally {
//            UserContext.clear();
//            System.out.println("🏁 REQUEST END: " + path);
//            System.out.println("==============================\n");
//        }
//    }
//
//    // ============================
//    // TOKEN EXTRACTION
//    // ============================
//    private String extractToken(HttpServletRequest request) {
//
//        Cookie[] cookies = request.getCookies();
//
//        if (cookies != null) {
//            for (Cookie cookie : cookies) {
//                if ("accessToken".equals(cookie.getName())) {
//                    System.out.println("🍪 Token found in cookie");
//                    return cookie.getValue();
//                }
//            }
//        }
//
//        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
//
//        if (authHeader != null && authHeader.startsWith("Bearer ")) {
//            System.out.println("📦 Token found in header");
//            return authHeader.substring(7);
//        }
//
//        System.out.println("⚠️ No token found in request");
//        return null;
//    }
//
//    // ============================
//    // PUBLIC ROUTES
//    // ============================
//    private boolean isPublicRoute(String path) {
//        return path.startsWith("/auth/")
//                || path.equals("/")
//                || path.equals("/error")
//                || path.equals("/favicon.ico")
//                || path.startsWith("/pricing")
//                || path.startsWith("/privacy")
//                || path.startsWith("/terms")
//                || path.equals("/users/oauth-login")
//                || path.equals("/users/demo-login")
//                || path.startsWith("/api/email/")
//                || path.equals("/api/s3/test");
//    }
//
//    // ============================
//    // SAFE STRING
//    // ============================
//    private String safe(String value, String fallback) {
//        return (value == null || value.isBlank()) ? fallback : value;
//    }
//}

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

        String path = request.getServletPath();

        logRequest(request);

        // =========================================================
        // PUBLIC ROUTE
        // =========================================================

        if (isPublicRoute(path)) {

            System.out.println("🔓 PUBLIC ROUTE");
            System.out.println("➡️ Continuing request");

            filterChain.doFilter(request, response);
            return;
        }

        // =========================================================
        // EXTRACT TOKEN
        // =========================================================

        String token = extractToken(request);

        if (token == null || token.isBlank()) {

            System.out.println("❌ AUTHENTICATION FAILED");
            System.out.println("Reason: TOKEN_MISSING");

            sendUnauthorized(
                    response,
                    "TOKEN_MISSING",
                    "Authentication token was not provided."
            );

            return;
        }

        System.out.println("🔐 JWT TOKEN FOUND");

        // =========================================================
        // VERIFY JWT
        // =========================================================

        DecodedJWT jwt;

        try {

            jwt = JWT.require(jwtConfig.algorithm())
                    .build()
                    .verify(token);

        } catch (Exception ex) {

            System.out.println("❌ JWT VERIFICATION FAILED");
            System.out.println("Exception: " + ex.getClass().getSimpleName());
            System.out.println("Message: " + ex.getMessage());

            sendUnauthorized(
                    response,
                    "INVALID_TOKEN",
                    "The authentication token is invalid or expired."
            );

            return;
        }

        // =========================================================
        // USER ID
        // =========================================================

        String subject = jwt.getSubject();

        if (subject == null || subject.isBlank()) {

            System.out.println("❌ JWT VERIFICATION FAILED");
            System.out.println("Reason: JWT subject is missing");

            sendUnauthorized(
                    response,
                    "INVALID_SUBJECT",
                    "Authentication token does not contain a valid user ID."
            );

            return;
        }

        UUID userId;

        try {

            userId = UUID.fromString(subject);

        } catch (IllegalArgumentException ex) {

            System.out.println("❌ JWT VERIFICATION FAILED");
            System.out.println("Invalid user ID in JWT: " + subject);

            sendUnauthorized(
                    response,
                    "INVALID_USER_ID",
                    "Authentication token contains an invalid user ID."
            );

            return;
        }

        // =========================================================
        // ROLES
        // =========================================================

        String accessRole =
                safe(jwt.getClaim("accessRole").asString(), "USER");

        String appRole =
                safe(jwt.getClaim("appRole").asString(), "MEMBER");

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        authorities.add(
                new SimpleGrantedAuthority(
                        "ROLE_" + accessRole.toUpperCase()
                )
        );

        authorities.add(
                new SimpleGrantedAuthority(
                        "APP_" + appRole.toUpperCase()
                )
        );

        // =========================================================
        // AUTHENTICATION
        // =========================================================

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        authorities
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        UserContext.setCurrentUser(userId);

        // =========================================================
        // AUTH SUCCESS
        // =========================================================

        System.out.println("✅ AUTHENTICATION SUCCESS");
        System.out.println("User ID: " + userId);
        System.out.println("Access Role: " + accessRole);
        System.out.println("App Role: " + appRole);
        System.out.println("Token Subject: " + jwt.getSubject());
        System.out.println("JWT Issuer: " + jwt.getIssuer());
        System.out.println("JWT Expires: " + jwt.getExpiresAt());
        System.out.println("========================================");

        // =========================================================
        // CONTINUE REQUEST
        //
        // IMPORTANT:
        // Do NOT wrap this in the JWT try/catch.
        //
        // If the controller/service throws an exception,
        // Spring should handle that exception instead of this
        // filter converting it into AUTH_FILTER_ERROR.
        // =========================================================

        try {

            filterChain.doFilter(request, response);

        } finally {

            UserContext.clear();
            SecurityContextHolder.clearContext();

            System.out.println("🏁 REQUEST COMPLETE");
            System.out.println(
                    "HTTP STATUS: " + response.getStatus()
            );
            System.out.println(
                    "➡️ " + request.getMethod() + " " + path
            );
            System.out.println("========================================\n");
        }
    }

    // =========================================================
    // REQUEST LOGGING
    // =========================================================

    private void logRequest(HttpServletRequest request) {

        System.out.println("\n========================================");
        System.out.println("🌐 API REQUEST");
        System.out.println("METHOD: " + request.getMethod());
        System.out.println("URI: " + request.getRequestURI());
        System.out.println("QUERY: " + request.getQueryString());

        System.out.println(
                "AUTHORIZATION: "
                        + (
                        request.getHeader(HttpHeaders.AUTHORIZATION) != null
                                ? "PRESENT"
                                : "MISSING"
                )
        );

        System.out.println(
                "COOKIE accessToken: "
                        + (
                        hasAccessTokenCookie(request)
                                ? "PRESENT"
                                : "MISSING"
                )
        );

        System.out.println(
                "CONTENT-TYPE: "
                        + request.getContentType()
        );

        System.out.println(
                "CONTENT-LENGTH: "
                        + request.getContentLengthLong()
        );

        System.out.println("========================================");
    }

    // =========================================================
    // TOKEN EXTRACTION
    // =========================================================

    private String extractToken(HttpServletRequest request) {

        // ---------------------------------------------------------
        // 1. Authorization header
        // ---------------------------------------------------------

        String authHeader =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && !authHeader.isBlank()) {

            System.out.println("📦 Authorization header found");

            if (authHeader.startsWith("Bearer ")) {

                String token = authHeader.substring(7).trim();

                if (!token.isBlank()) {
                    System.out.println("✅ Bearer token extracted");
                    return token;
                }
            }

            System.out.println("⚠️ Authorization header is malformed");
        }

        // ---------------------------------------------------------
        // 2. Cookie
        // ---------------------------------------------------------

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {

            for (Cookie cookie : cookies) {

                if ("accessToken".equals(cookie.getName())) {

                    String token = cookie.getValue();

                    if (token != null && !token.isBlank()) {

                        System.out.println(
                                "🍪 accessToken cookie found"
                        );

                        return token;
                    }
                }
            }
        }

        System.out.println("❌ No authentication token found");

        return null;
    }

    // =========================================================
    // COOKIE CHECK
    // =========================================================

    private boolean hasAccessTokenCookie(
            HttpServletRequest request
    ) {

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return false;
        }

        for (Cookie cookie : cookies) {

            if ("accessToken".equals(cookie.getName())) {
                return true;
            }
        }

        return false;
    }

    // =========================================================
    // UNAUTHORIZED RESPONSE
    // =========================================================

    private void sendUnauthorized(
            HttpServletResponse response,
            String error,
            String message
    ) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String json =
                "{"
                        + "\"error\":\"" + error + "\","
                        + "\"message\":\"" + message + "\""
                        + "}";

        response.getWriter().write(json);
    }

    // =========================================================
    // PUBLIC ROUTES
    // =========================================================

    private boolean isPublicRoute(String path) {

        return path.startsWith("/auth/")
                || path.equals("/")
                || path.equals("/error")
                || path.equals("/favicon.ico")
                || path.startsWith("/pricing")
                || path.startsWith("/privacy")
                || path.startsWith("/terms")
                || path.equals("/users/oauth-login")
                || path.equals("/users/demo-login")
                || path.startsWith("/api/email/")
                || path.equals("/api/s3/test");
    }

    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safe(
            String value,
            String fallback
    ) {

        return value == null || value.isBlank()
                ? fallback
                : value;
    }
}