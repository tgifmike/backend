package com.backend.backend.service;

import com.backend.backend.dto.LoginResponse;
import com.backend.backend.dto.UserDto;
import com.backend.backend.dto.UserMeResponse;
import com.backend.backend.entity.UserEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface UserService {
   //create user
    UserEntity createUser(UserEntity user);
    UserEntity getUserById(UUID id);
    UserEntity findByEmail(String email);

    UserEntity updateUser(UserEntity user);
    void deleteUser(UUID id);

    //get all users
    List<UserEntity> getAllUsers();

    //toggle active
    UserDto toggleActive(UUID id, boolean active);

    //update user access role
    UserDto updateAccessRole(UUID id, String role);

    //update user app role
    UserDto updateAppRole(UUID id, String role);

    //update user
    void updateUser(UUID id, String newName, String newEmail);
    boolean isEmailDuplicate(String email, UUID excludeId);
    boolean isNameDuplicate(String name, UUID excludeId);
//    UserEntity createOrFindGoogleUser(String email, String name, String googleId, String picture);
//    UserEntity createOrFindOAuthUser(UserEntity incomingUser);
    String generateJwtForUser(UserEntity user);
   // void validateUserAccess(UserEntity user);
//    String handleOAuthLogin(UserEntity incomingUser);
   UserEntity inviteUser(
           String email,
           String appRole,
           String accessRole,
           String accountId,
           String inviterName
   );
   // void validateUserAccess(UUID userId);
    LoginResponse handleOAuthLogin(UserEntity incomingUser);
    UserEntity resolveUserIdentity(UserEntity incomingUser);
    UserEntity createDemoUser();

    UserMeResponse getCurrentUser();
}