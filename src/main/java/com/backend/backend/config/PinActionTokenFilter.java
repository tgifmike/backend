package com.backend.backend.config;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.backend.backend.security.PinActionPrincipal;
import com.backend.backend.config.UserContext;
import com.backend.backend.service.PinActionTokenService;
import com.backend.backend.service.PinActionAuthorizationService;
import com.backend.backend.exception.PinApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PinActionTokenFilter extends OncePerRequestFilter {
    private final PinActionTokenService tokenService;
    private final PinActionAuthorizationService authorizationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // PIN verification starts a new online authentication flow.  A client
        // may have retained an earlier employee token in its Authorization
        // interceptor; do not interpret that token as a PIN action on this
        // public endpoint.
        if (isPinVerificationEndpoint(request)) {
            chain.doFilter(request, response);
            return;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                PinActionPrincipal principal = tokenService.verify(authorization.substring(7).trim());
                if (!isAllowedPinAction(request, principal)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"PIN_TOKEN_SCOPE_FORBIDDEN\"}");
                    return;
                }
                authorizationService.validateToken(principal);
                UUID selectedLocation = selectedLocationPath(request);
                if (selectedLocation != null) {
                    authorizationService.validateLocationSelection(principal, selectedLocation);
                }
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("PIN_LINE_CHECK"))
                        )
                );
                UserContext.setCurrentUser(principal.userId());
                try {
                    chain.doFilter(request, response);
                } finally {
                    UserContext.clear();
                    SecurityContextHolder.clearContext();
                }
                return;
            } catch (PinApiException exception) {
                response.setStatus(exception.getStatus().value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"" + exception.getCode() + "\"}");
                return;
            } catch (JWTVerificationException | IllegalArgumentException ignored) {
                // It may be a manager OAuth JWT; the OAuth filter verifies it next.
            }
        }
        chain.doFilter(request, response);
    }

    private static boolean isPinVerificationEndpoint(HttpServletRequest request) {
        String path = normalizedPath(request);
        return "POST".equals(request.getMethod()) && "/auth/pin/verify".equals(path);
    }

    private static boolean isAllowedPinAction(HttpServletRequest request, PinActionPrincipal principal) {
        String path = normalizedPath(request);
        if ("POST".equals(request.getMethod())
                && (path.equals("/line-checks/create")
                    || path.equals("/line-checks/save"))) {
            return true;
        }
        if ("PATCH".equals(request.getMethod())
                && path.matches("/line-check-items/[0-9a-fA-F-]+/correction")) {
            return true;
        }
        if ("GET".equals(request.getMethod())) {
            if (path.matches("/api/line-check-items/[0-9a-fA-F-]+/photos")) {
                return true;
            }
            if (isUserScopedRead(path, "/user-access/", "/accounts", principal)
                    || isUserScopedRead(path, "/user-access-locations/", "/locations", principal)) {
                return true;
            }
            String prefix = "/locations/accounts/";
            if (path.startsWith(prefix) && path.endsWith("/locations")) {
                String accountId = path.substring(prefix.length(), path.length() - "/locations".length());
                try {
                    return UUID.fromString(accountId).equals(principal.accountId());
                } catch (IllegalArgumentException ignored) {
                    return false;
                }
            }
            String stationPrefix = "/stations/by-location/";
            if (path.startsWith(stationPrefix)) {
                try {
                    UUID.fromString(path.substring(stationPrefix.length()));
                    return true;
                } catch (IllegalArgumentException ignored) {
                    return false;
                }
            }
            String historyPrefix = "/line-checks/completed/by-location/";
            if (path.startsWith(historyPrefix)) {
                try {
                    UUID.fromString(path.substring(historyPrefix.length()));
                    return true;
                } catch (IllegalArgumentException ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    private static boolean isUserScopedRead(
            String path, String prefix, String suffix, PinActionPrincipal principal) {
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
            return false;
        }
        String userId = path.substring(prefix.length(), path.length() - suffix.length());
        try {
            return UUID.fromString(userId).equals(principal.userId());
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static UUID selectedLocationPath(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod())) {
            return null;
        }
        String path = normalizedPath(request);
        String[] prefixes = {"/stations/by-location/", "/line-checks/completed/by-location/"};
        for (String prefix : prefixes) {
            if (path.startsWith(prefix)) {
                try {
                    return UUID.fromString(path.substring(prefix.length()));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String normalizedPath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }
        }
        if (path == null || path.isBlank()) {
            return "";
        }
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
