package com.backend.backend.service.apple;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.backend.backend.dto.apple.AppleTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Service
public class AppleOAuthService {

    @Value("${apple.client.id}")
    private String clientId;

    @Value("${apple.team.id}")
    private String teamId;

    @Value("${apple.key.id}")
    private String keyId;

    @Value("${apple.redirect.uri}")
    private String appleRedirectUri;

    @Value("${apple.private.key}")
    private String privateKey; // .p8 contents

    private final RestTemplate restTemplate = new RestTemplate();



    //apple helper
    public AppleTokenResponse exchangeCodeForToken(String code) {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", generateClientSecret());
        form.add("code", code);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", appleRedirectUri);

        System.out.println("APPLE TOKEN REDIRECT URI = " + appleRedirectUri);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(form, headers);



        return restTemplate.postForObject(
                "https://appleid.apple.com/auth/token",
                request,
                AppleTokenResponse.class
        );
    }

    public String generateClientSecret() {
        try {
            ECPrivateKey ecPrivateKey = loadPrivateKey(privateKey);

            Date now = new Date();
            Date exp = new Date(now.getTime() + 1000L * 60 * 60 * 24 * 180);

            Algorithm algorithm = Algorithm.ECDSA256(null, ecPrivateKey);

            return JWT.create()
                    .withKeyId(keyId)
                    .withIssuer(teamId)
                    .withAudience("https://appleid.apple.com")
                    .withSubject(clientId)
                    .withIssuedAt(now)
                    .withExpiresAt(exp)
                    .sign(algorithm);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Apple client secret", e);
        }
    }

    private ECPrivateKey loadPrivateKey(String pem) throws Exception {
        String cleaned = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\n", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(cleaned);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);

        KeyFactory keyFactory = KeyFactory.getInstance("EC");

        return (ECPrivateKey) keyFactory.generatePrivate(spec);
    }
}

