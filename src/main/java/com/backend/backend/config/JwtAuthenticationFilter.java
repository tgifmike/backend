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

    private static final List<String> PUBLIC_PATHS = List.of(
            "/",
            "/privacy",
            "/terms",
            "/contact",
            "/auth/google/login",
            "/auth/google/callback",
            "/error",
            "/favicon.ico"
    );

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

        System.out.println("🔥 JWT FILTER HIT: " + request.getRequestURI());
        try {

            if (isPublic(path)) {
                filterChain.doFilter(request, response);
                return;
            }

            String header = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (header == null || !header.startsWith("Bearer ")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            String token = header.substring(7);

            DecodedJWT jwt = JWT.require(jwtConfig.algorithm())
                    .build()
                    .verify(token);

            String userId = jwt.getSubject();

            if (userId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            System.out.println("✅ JWT AUTH SUCCESS: " + userId);

            UUID uuid = UUID.fromString(userId);

            // 🔥 THIS IS THE MISSING PIECE
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            uuid,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );

            SecurityContextHolder.getContext().setAuthentication(auth);

            UserContext.setCurrentUser(uuid);

            filterChain.doFilter(request, response);

        } catch (Exception e) {

            System.out.println("❌ JWT ERROR: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        } finally {
            UserContext.clear();
        }
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.contains(path);
    }
}