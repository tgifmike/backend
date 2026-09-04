package com.backend.backend.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Set;

@Component
public class AppleIdTokenVerifier {
    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final String APPLE_JWKS = "https://appleid.apple.com/auth/keys";

    private final ConfigurableJWTProcessor<SecurityContext> processor;

    public AppleIdTokenVerifier(@Value("${apple.client.id}") String clientId) {
        try {
            JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(APPLE_JWKS));
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
            DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            jwtProcessor.setJWSKeySelector(keySelector);
            JWTClaimsSet requiredClaims = new JWTClaimsSet.Builder()
                    .issuer(APPLE_ISSUER)
                    .audience(clientId)
                    .build();
            jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                    requiredClaims,
                    Set.of("sub", "iat", "exp")
            ));
            this.processor = jwtProcessor;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to initialize Apple ID token verification", ex);
        }
    }

    public JWTClaimsSet verify(String token) {
        try {
            return processor.process(token, null);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Apple identity token", ex);
        }
    }
}
