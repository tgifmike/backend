package com.backend.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserMeResponse {

    private String id;
    private String name;
    private String email;
    private String image;
    private String appRole;
    private String accessRole;

}
