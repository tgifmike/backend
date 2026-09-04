package com.backend.backend.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.JWTVerifier.BaseVerification;
import com.backend.backend.security.PinActionPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class PinActionTokenService {
    public static final Duration TOKEN_LIFETIME = Duration.ofMinutes(10);

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final String issuer;
    private final String audience;
    private final Clock clock;

    public PinActionTokenService(
            @Value("${pin.action-token-secret}") String secret,
            @Value("${pin.action-token-issuer:the-manager-life-pin}") String issuer,
            @Value("${pin.action-token-audience:the-manager-life-line-check}") String audience,
            Clock clock
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("PIN_ACTION_TOKEN_SECRET must contain at least 32 characters");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.audience = audience;
        this.clock = clock;
        BaseVerification verification = (BaseVerification) JWT.require(algorithm);
        verification.withIssuer(issuer);
        verification.withAudience(audience);
        verification.withClaim("authMethod", "PIN");
        verification.withClaim("scope", "LINE_CHECK");
        this.verifier = verification.build(clock);
    }

    public String issue(UUID userId, UUID accountId, UUID locationId, UUID deviceId, long credentialVersion) {
        Instant issuedAt = clock.instant();
        return JWT.create()
                .withSubject(userId.toString())
                .withIssuer(issuer)
                .withAudience(audience)
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(issuedAt.plus(TOKEN_LIFETIME)))
                .withClaim("accountId", accountId.toString())
                .withClaim("locationId", locationId == null ? null : locationId.toString())
                .withClaim("deviceId", deviceId.toString())
                .withClaim("authMethod", "PIN")
                .withClaim("scope", "LINE_CHECK")
                .withClaim("credentialVersion", credentialVersion)
                .sign(algorithm);
    }

    public PinActionPrincipal verify(String token) {
        DecodedJWT jwt = verifier.verify(token);
        return new PinActionPrincipal(
                UUID.fromString(jwt.getSubject()),
                UUID.fromString(jwt.getClaim("accountId").asString()),
                parseOptionalUuid(jwt.getClaim("locationId").asString()),
                UUID.fromString(jwt.getClaim("deviceId").asString()),
                jwt.getClaim("credentialVersion").asLong()
        );
    }

    private static UUID parseOptionalUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }
}
