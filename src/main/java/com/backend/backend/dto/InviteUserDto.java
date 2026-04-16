package com.backend.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InviteUserDto {

    private String email;
    private String appRole;
    private String accessRole;
    private String accountId;
    private String inviterName;
}
