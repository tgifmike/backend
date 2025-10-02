package com.backend.backend.dto;

import com.backend.backend.config.AccessRole;
import com.backend.backend.config.AppRole;
import lombok.*;

import java.time.LocalDateTime;
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
    private String accessRole;
    private String appRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

