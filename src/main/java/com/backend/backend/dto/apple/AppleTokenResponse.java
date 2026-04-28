package com.backend.backend.dto.apple;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AppleTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("id_token")
    private String idToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Integer expiresIn;

    public String getAccessToken() { return accessToken; }
    public String getIdToken() { return idToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getTokenType() { return tokenType; }
    public Integer getExpiresIn() { return expiresIn; }

    public void setAccessToken(String v) { accessToken = v; }
    public void setIdToken(String v) { idToken = v; }
    public void setRefreshToken(String v) { refreshToken = v; }
    public void setTokenType(String v) { tokenType = v; }
    public void setExpiresIn(Integer v) { expiresIn = v; }
}