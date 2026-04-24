package com.backend.backend.service.google;


import com.backend.backend.dto.google.GoogleTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GoogleOAuthService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    /**
     * Exchange authorization code for access token
     */
    public GoogleTokenResponse exchangeCodeForToken(String code) {

        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
//        params.add("client_id", googleClientId);
//        params.add("client_secret", googleClientSecret);
//        params.add("redirect_uri", googleRedirectUri);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(params, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        "https://oauth2.googleapis.com/token",
                        request,
                        Map.class
                );

        Map body = response.getBody();

        GoogleTokenResponse token = new GoogleTokenResponse();
        token.setAccessToken((String) body.get("access_token"));
        token.setIdToken((String) body.get("id_token"));
        token.setRefreshToken((String) body.get("refresh_token"));

        return token;
    }
}
