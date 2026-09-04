package com.backend.backend.config;

import com.backend.backend.exception.PinApiException;
import com.backend.backend.security.DeviceAuthenticationPrincipal;
import com.backend.backend.service.IpadDeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeviceAuthenticationFilter extends OncePerRequestFilter {
    private final IpadDeviceService deviceService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String deviceIdHeader = request.getHeader("X-Device-Id");
        boolean deviceAuthorization = authorization != null && authorization.startsWith("Device ");
        if (!deviceAuthorization && deviceIdHeader == null) {
            chain.doFilter(request, response);
            return;
        }
        DeviceAuthenticationPrincipal principal;
        try {
            if (!deviceAuthorization || deviceIdHeader == null) {
                throw new PinApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "INVALID_DEVICE", "Both device credentials are required");
            }
            UUID deviceId = UUID.fromString(deviceIdHeader);
            principal = deviceService.authenticate(
                    deviceId,
                    authorization.substring("Device ".length()).trim()
            );
            if (!isDeviceRoute(request, deviceId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                objectMapper.writeValue(response.getWriter(), Map.of("error", "DEVICE_SCOPE_FORBIDDEN"));
                return;
            }
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            objectMapper.writeValue(response.getWriter(), Map.of("error", "INVALID_DEVICE"));
            return;
        }
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("DEVICE_AUTH"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }

    private static boolean isDeviceRoute(HttpServletRequest request, UUID deviceId) {
        String prefix = "/ipad/devices/" + deviceId;
        return ("GET".equals(request.getMethod()) && request.getServletPath().equals(prefix + "/pin-verifiers"))
                || ("POST".equals(request.getMethod()) && request.getServletPath().equals(prefix + "/pin-events/batch"));
    }
}
