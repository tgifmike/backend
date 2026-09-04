package com.backend.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Component
public class AuditRequestMetadataProvider {
    public AuditMetadata current() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return new AuditMetadata(null, null, UUID.randomUUID().toString());
        }
        HttpServletRequest request = attributes.getRequest();
        // RemoteIpValve/framework forwarding configuration is responsible for
        // trusted proxy handling. Never trust a client-supplied X-Forwarded-For directly.
        String ip = request.getRemoteAddr();
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return new AuditMetadata(ip, truncate(request.getHeader("User-Agent"), 512), truncate(correlationId, 128));
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public record AuditMetadata(String ipAddress, String userAgent, String correlationId) {
    }
}
