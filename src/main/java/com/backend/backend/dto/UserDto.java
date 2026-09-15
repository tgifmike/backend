package com.backend.backend.dto;

import com.backend.backend.enums.AuthenticationMode;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private UUID id;
    private String userName;
    private String userEmail;
    private String userImage;
    private boolean userActive;
    private boolean firstLogin;
    private boolean invited;
    private String accessRole;
    private String appRole;
    private AuthenticationMode authenticationMode;
    private Instant createdAt;
    private Instant updatedAt;
}
