package com.backend.backend.repositories;

import com.backend.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

 // =========================
 // SOFT-DELETED SAFE READS
 // =========================

 List<UserEntity> findAllByDeletedAtIsNull();

 Optional<UserEntity> findByIdAndDeletedAtIsNull(UUID id);

 Optional<UserEntity> findByUserEmailIgnoreCaseAndDeletedAtIsNull(String email);

 Optional<UserEntity> findByGoogleIdAndDeletedAtIsNull(String googleId);

 Optional<UserEntity> findByAppleIdAndDeletedAtIsNull(String appleId);


 // =========================
 // DUPLICATE CHECKS (ACTIVE ONLY)
 // =========================

 boolean existsByUserEmailAndDeletedAtIsNull(String email);

 boolean existsByUserNameAndDeletedAtIsNull(String name);

 boolean existsByUserEmailAndIdNotAndDeletedAtIsNull(String email, UUID id);

 boolean existsByUserNameAndIdNotAndDeletedAtIsNull(String name, UUID id);


 // =========================
 // ADMIN / RAW (USE CAREFULLY)
 // =========================

 // only if you EVER need to recover deleted users
 Optional<UserEntity> findByUserEmailIgnoreCase(String email);

 Optional<UserEntity> findByGoogleId(String googleId);

 Optional<UserEntity> findByAppleId(String appleId);
}
