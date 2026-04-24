package com.backend.backend.service.google;

import com.backend.backend.config.GoogleOAuthConfig;
import com.backend.backend.dto.google.GoogleUserInfo;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GoogleUserInfoService {

    private final RestTemplate restTemplate = new RestTemplate();

    public GoogleUserInfo fetchUserInfo(String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<GoogleUserInfo> response =
                restTemplate.exchange(
                        GoogleOAuthConfig.USERINFO_URL,
                        HttpMethod.GET,
                        request,
                        GoogleUserInfo.class
                );

        if (!response.getStatusCode().is2xxSuccessful()
                || response.getBody() == null) {
            throw new RuntimeException("Failed to fetch Google user info");
        }

        return response.getBody();
    }
}
