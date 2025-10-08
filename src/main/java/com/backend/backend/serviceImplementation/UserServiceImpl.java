package com.backend.backend.serviceImplementation;

import com.backend.backend.config.AccessRole;
import com.backend.backend.config.AppRole;
import com.backend.backend.dto.UserDto;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.repositories.UserRepository;
import com.backend.backend.service.UserService;
import jakarta.transaction.Transactional;
import org.apache.catalina.User;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    //create user from button
    @Override
    public UserEntity createUser(UserEntity user) {
        // Prevent duplicate emails
        if (userRepository.existsByUserEmail(user.getUserEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.existsByUserName(user.getUserName())) {
            throw new IllegalArgumentException("User name already exists");
        }

        // Ensure required fields
        if (user.getUserName() == null || user.getUserName().isBlank()) {
            throw new RuntimeException("Name cannot be empty");
        }
        if (user.getUserEmail() == null || user.getUserEmail().isBlank()) {
            throw new RuntimeException("Email cannot be empty");
        }

        // Defaults (if not already set)
        if (user.getAccessRole() == null) {
            user.setAccessRole(AccessRole.USER);
        }
        if (user.getAppRole() == null) {
            user.setAppRole(AppRole.MEMBER);
        }
        user.setUserActive(true);
        user.setFirstLogin(true);

        return userRepository.save(user);
    }


    //get all users
    @Override
    public List<UserEntity> getAllUsers(){
        return userRepository.findAll();
    }

    //find by id
    @Override
    public UserEntity getUserById(UUID id){
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id " + id));
    }

    //update user name and email
    @Override
    public UserEntity updateUser(UserEntity user){
        return userRepository.save(user);
    }

    //find by email
    @Override
    public UserEntity findByEmail(String email){
        return userRepository.findByUserEmail(email)
                .orElseThrow(()-> new RuntimeException("User not found with email " + email));
    }

    //delete user
    @Override
    public void deleteUser(UUID id){
        userRepository.deleteById(id);
    }

    //toggle active
    @Override
    @Transactional
    public UserDto toggleActive(UUID id, boolean active) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        user.setUserActive(active);
        user.setUpdatedAt(LocalDateTime.now());

        UserEntity saved = userRepository.save(user);

        // Manually map Entity → DTO
        return new UserDto(
                saved.getId(),
                saved.getUserName(),
                saved.getUserEmail(),
                saved.getUserImage(),
                saved.isUserActive(),
                saved.getAccessRole() != null ? saved.getAccessRole().name() : null,
                saved.getAppRole() != null ? saved.getAppRole().name() : null,
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    //update user access role
    @Override
    public UserDto updateAccessRole(UUID id, String role) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            user.setAccessRole(AccessRole.valueOf(role.toUpperCase())); // convert string safely
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid access role: " + role);
        }

        UserEntity saved = userRepository.save(user);

        return new UserDto(
                saved.getId(),
                saved.getUserName(),
                saved.getUserEmail(),
                saved.getUserImage(),
                saved.isUserActive(),
                saved.getAccessRole() != null ? saved.getAccessRole().name() : null,
                saved.getAppRole() != null ? saved.getAppRole().name() : null,
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    //update app role
    @Override
    public UserDto updateAppRole(UUID id, String role){
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User no found"));
        user.setAppRole(AppRole.valueOf(role));
        UserEntity saved = userRepository.save(user);
        return new UserDto(
                saved.getId(),
                saved.getUserName(),
                saved.getUserEmail(),
                saved.getUserImage(),
                saved.isUserActive(),
                saved.getAccessRole() != null ? saved.getAccessRole().name() : null,
                saved.getAppRole() != null ? saved.getAppRole().name() : null,
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    //update user name an email
    @Override
    public void updateUser(UUID id, String newName, String newEmail){
        //validate input
        if (newName == null || newName.trim().isEmpty()){
            throw new IllegalArgumentException("User name cannot be blank");
        }
        if (newEmail == null || newEmail.trim().isEmpty()){
            throw new IllegalArgumentException(("User email cannot be blank"));
        }

        // Check for duplicates
        if (isNameDuplicate(newName, id)) {
            throw new IllegalArgumentException("User name already exists");
        }
        if (isEmailDuplicate(newEmail, id)) {
            throw new IllegalArgumentException("User email already exists");
        }

        Optional<UserEntity> userOpt = userRepository.findById(id);

        if(userOpt.isPresent()){
            UserEntity user = userOpt.get();
            user.setUserName(newName);
            user.setUserEmail(newEmail);
            userRepository.save(user);
        } else {
            throw new RuntimeException("User not found with id: " + id);
        }


        }

    @Override
    public boolean isEmailDuplicate(String email, UUID excludeId) {
        return userRepository.existsByUserEmailAndIdNot(email, excludeId);
    }

    @Override
    public boolean isNameDuplicate(String name, UUID excludeId) {
        return userRepository.existsByUserNameAndIdNot(name, excludeId);
    }

    @Override
    public UserEntity createOrFindGoogleUser(UserEntity user) {
        Optional<UserEntity> existingUser = userRepository.findByUserEmail(user.getUserEmail());

        if (existingUser.isPresent()) {
            // user already exists, just return
            return existingUser.get();
        }

        user.setUserActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setAccessRole(AccessRole.USER);  // default
        user.setAppRole(AppRole.MEMBER);     // default

        return userRepository.save(user);
    }



}
