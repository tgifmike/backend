package com.backend.backend.dto.google;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleUserInfo {

    private String sub;
    private String email;
    private String name;
    private String picture;
    private Boolean email_verified;

    // getters + setters
}
