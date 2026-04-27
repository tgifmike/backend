package com.backend.backend.dto.apple;

public class AppleTokenResponse {
    private String access_token;
    private String id_token;
    private String refresh_token;
    private Integer expires_in;

    public String getIdToken() {
        return id_token;
    }
}
