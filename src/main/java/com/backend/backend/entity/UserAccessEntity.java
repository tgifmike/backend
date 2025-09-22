package com.backend.backend.entity;


import com.backend.backend.config.AccessRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name= "user_access")
public class UserAccessEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    @ManyToOne(optional = true)
    @JoinColumn(name = "location_id", nullable = true)
    private LocationEntity location;

    // optional: access scope or type
    @Enumerated(EnumType.STRING)
    private AccessRole accessRole; // USER, ADMIN, RESTRICTED, etc.
}
