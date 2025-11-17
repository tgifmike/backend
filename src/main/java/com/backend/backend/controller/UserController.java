package com.backend.backend.controller;

import com.backend.backend.config.GoogleTokenVerifier;
import com.backend.backend.dto.UpdateUserDto;
import com.backend.backend.dto.UserDto;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.repositories.UserRepository;
import com.backend.backend.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = {
        "http://localhost:3000",
        "https://www.themanagerlife.com"
})

@RestController
@RequestMapping("/users")
public class UserController {

    //private final UserRepository userRepository;
    private final UserService userService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public UserController(UserService userService, GoogleTokenVerifier googleTokenVerifier) {
        this.userService = userService;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    //get all users
    @GetMapping("/all")
    public List<UserEntity> getAllUsers() {return userService.getAllUsers();}

    //update user status
    @PatchMapping("/{id}/active")
    public ResponseEntity<UserDto> toggleActive(
            @PathVariable UUID id,
            @RequestParam boolean active
    ) {
        UserDto updated = userService.toggleActive(id, active);
        return ResponseEntity.ok(updated);
    }

    //update user access role
    @PatchMapping("/{id}/accessRole")
    public ResponseEntity<UserDto> updateAccessRole(
            @PathVariable UUID id,
            @RequestParam String role
    ) {
        UserDto updated = userService.updateAccessRole(id, role);
        return ResponseEntity.ok(updated);
    }

    //update user app access
    @PatchMapping("/{id}/appRole")
    public ResponseEntity<UserDto> updateAppRole(
            @PathVariable UUID id,
            @RequestParam String role
    ) {
        UserDto updated = userService.updateAppRole(id, role);
        return ResponseEntity.ok(updated);
    }

    //delete user
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.notFound().build(); // 404 if user not found
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 on other errors
        }
    }

    //edit user name and email
    @PutMapping("/update/{id}")
    public ResponseEntity<String> updateUser(
            @PathVariable UUID id,
            @RequestBody UpdateUserDto request
    ) {
        userService.updateUser(id, request.getName(), request.getEmail());
        return ResponseEntity.ok("User updated successfully");
    }

    //create user
    @PostMapping("/create")
    public ResponseEntity<?> createUser(@RequestBody UserEntity user) {
        try {
            UserEntity savedUser = userService.createUser(user);
            return ResponseEntity.ok(savedUser);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body("Failed to create user");
        }
    }

    @PostMapping("/create/google")
    public ResponseEntity<?> createGoogleUser(@RequestBody UserEntity user) {
        try {
            UserEntity savedUser = userService.createOrFindGoogleUser(user);
            return ResponseEntity.ok(savedUser);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body("Failed to create Google user");
        }
    }

    @PostMapping("/mobile")
    public ResponseEntity<?> signInWithGoogle(@RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");

        try {
            // 1. Verify Google ID Token
            GoogleIdToken.Payload payload = googleTokenVerifier.verify(idToken);

            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");
            String googleId = payload.getSubject(); // Unique Google user ID

            // 2. Create or fetch existing user
            UserEntity user = userService.createOrFindGoogleUser(
                    email, name, googleId, picture
            );

            // 3. Generate JWT for your Android app
            String jwt = userService.generateJwtForUser(user);

            return ResponseEntity.ok(Map.of(
                    "token", jwt,
                    "user", user
            ));

        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid Google token");
        }
    }
}
