package com.backend.backend.dto;

import lombok.*;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountDto {
    private UUID id;
    private String accountName;
    private String imageBase64;
    private boolean accountActive;
}
