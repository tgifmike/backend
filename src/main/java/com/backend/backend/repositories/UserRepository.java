
package com.backend.backend.repositories;

import com.backend.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByUserEmail(String email);

    //checking for duplicates when updating
    boolean existsByUserEmailAndIdNot(String email, UUID id);
    boolean existsByUserNameAndIdNot(String userName, UUID id);

    //creating user checking if user exist by email
    boolean existsByUserEmail(String userEmail);
    boolean existsByUserName(String name);
}