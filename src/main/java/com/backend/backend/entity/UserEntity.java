package com.backend.backend.entity;

import com.backend.backend.enums.AccessRole;
import com.backend.backend.enums.AppRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
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

    @Column()
    private String provider;

    @Column(unique = true)
    private String providerAccountId;

    @Column(unique = true)
    private String googleId;

    @Column(unique = true)
    private String appleId;

    boolean invited;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_role")
    private AccessRole accessRole = AccessRole.USER;

    @Enumerated(EnumType.STRING)
    @Column()
    private AppRole appRole = AppRole.MEMBER;

    @Column(name = "created_at", updatable = false)
    //private LocalDateTime createdAt;
    private Instant createdAt;

    @Column(name = "updated_at")
   // private LocalDateTime updatedAt;
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
