package com.backend.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String userIdHeader = request.getHeader("X-User-Id"); // frontend must send this
            if (userIdHeader != null) {
                UserContext.setCurrentUser(UUID.fromString(userIdHeader));
            }
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}

