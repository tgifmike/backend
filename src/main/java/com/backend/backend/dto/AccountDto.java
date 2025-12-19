package com.backend.backend.dto;

import com.backend.backend.entity.AccountEntity;
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

    public AccountDto(AccountEntity account) {
        this.id = account.getId();
        this.accountName = account.getAccountName();
        this.imageBase64 = account.getImageBase64();
        this.accountActive = account.getAccountActive();
    }

}
