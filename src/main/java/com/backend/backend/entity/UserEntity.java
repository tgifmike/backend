package com.backend.backend.entity;

import com.backend.backend.config.AccessRole;
import com.backend.backend.config.AppRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name= "users")
public class UserEntity {


    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    private String userName;
    @Column(name="user_email", unique = true)

    private String userEmail;
    private String userImage;
    private boolean userActive = true;
    private boolean firstLogin = true;

    @Column(unique = true)
    private String googleId;

//    @ManyToMany
//    @JoinTable(
//            name = "user_account_access",
//            joinColumns = @JoinColumn(name = "user_id"),
//            inverseJoinColumns = @JoinColumn(name = "account_id")
//    )
//    private Set<AccountEntity> accessibleAccounts;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_role")
    private AccessRole accessRole = AccessRole.USER;

    @Enumerated(EnumType.STRING)
    @Column()
    private AppRole appRole = AppRole.MEMBER;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
