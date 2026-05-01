package com.backend.backend.controller;

import com.backend.backend.config.GoogleTokenVerifier;
import com.backend.backend.config.UserContext;
import com.backend.backend.dto.*;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.entity.UserHistoryEntity;
import com.backend.backend.repositories.UserHistoryRepository;
import com.backend.backend.repositories.UserRepository;
import com.backend.backend.service.TokenService;
import com.backend.backend.service.UserService;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.*;

@CrossOrigin(origins = {
        "http://localhost:3000",
        "https://www.themanagerlife.com"
})
@RestController
@RequestMapping("/users")
public class UserController {



    private final UserService userService;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final UserHistoryRepository userHistoryRepository;



    public UserController(
            UserService userService,
            UserRepository userRepository,
            TokenService tokenService,
            UserHistoryRepository userHistoryRepository

    ) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.userHistoryRepository = userHistoryRepository;
    }


//////////////////////////////////////////////////////////////
// ADMIN USER MANAGEMENT ENDPOINTS

    /// ///////////////////////////////////////////////////////////

    @GetMapping("/all")
    public List<UserEntity> getAllUsers() {
        return userService.getAllUsers();
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<UserDto> toggleActive(
            @PathVariable UUID id,
            @RequestParam boolean active
    ) {
        return ResponseEntity.ok(
                userService.toggleActive(id, active)
        );
    }

    @PatchMapping("/{id}/accessRole")
    public ResponseEntity<UserDto> updateAccessRole(
            @PathVariable UUID id,
            @RequestParam String role
    ) {
        return ResponseEntity.ok(
                userService.updateAccessRole(id, role)
        );
    }

    @PatchMapping("/{id}/appRole")
    public ResponseEntity<UserDto> updateAppRole(
            @PathVariable UUID id,
            @RequestParam String role
    ) {
        return ResponseEntity.ok(
                userService.updateAppRole(id, role)
        );
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID id
    ) {

        try {

            userService.deleteUser(id);

            return ResponseEntity.noContent().build();

        } catch (EmptyResultDataAccessException e) {

            return ResponseEntity.notFound().build();

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<String> updateUser(
            @PathVariable UUID id,
            @RequestBody UpdateUserDto request
    ) {

        userService.updateUser(
                id,
                request.getName(),
                request.getEmail()
        );

        return ResponseEntity.ok(
                "User updated successfully"
        );
    }

//////////////////////////////////////////////////////////////
// MANUAL USER CREATION

    /// ///////////////////////////////////////////////////////////

    @PostMapping("/create")
    public ResponseEntity<?> createUser(
            @RequestBody UserEntity user
    ) {

        try {

            return ResponseEntity.ok(
                    userService.createUser(user)
            );

        } catch (IllegalArgumentException ex) {

            return ResponseEntity
                    .badRequest()
                    .body(ex.getMessage());

        } catch (Exception ex) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create user");
        }
    }

    //////////////////////////////////////////////////////////////
// UNIVERSAL OAUTH LOGIN ENDPOINT

    /// /////////////////////////////////////////////////////////

    @PostMapping("/oauth-login")
    public ResponseEntity<?> loginWithOAuth(
            @RequestBody Map<String, Object> body
    ) {

        try {

            String provider =
                    ((String) body.get("provider")).toLowerCase();

            String idToken =
                    extractIdToken(body);

            UserEntity oauthUser =
                    new UserEntity();

            switch (provider) {

                case "google" -> {

                    var payload =
                            GoogleTokenVerifier.verifyToken(idToken);

                    if (payload == null)
                        return unauthorized("Invalid Google token");

                    oauthUser.setGoogleId(
                            payload.getSubject()
                    );

                    oauthUser.setUserEmail(
                            payload.getEmail()
                    );

                    oauthUser.setUserName(
                            (String) payload.get("name")
                    );

                    oauthUser.setUserImage(
                            (String) payload.get("picture")
                    );
                }

                case "apple" -> {

                    var jwt =
                            SignedJWT.parse(idToken);

                    oauthUser.setAppleId(
                            jwt.getJWTClaimsSet().getSubject()
                    );

                    oauthUser.setUserEmail(
                            (String)
                                    jwt.getJWTClaimsSet()
                                            .getClaim("email")
                    );
                }

                default -> throw new RuntimeException(
                        "Unsupported provider"
                );
            }

            LoginResponse response =
                    userService.handleOAuthLogin(oauthUser);

            System.out.println("PROVIDER = " + provider);
            System.out.println("TOKEN = " + idToken.substring(0,30));

            return ResponseEntity.ok(response);

        } catch (RuntimeException ex) {

            return switch (ex.getMessage()) {

                case "AccessDenied" -> forbidden("User not invited yet");

                case "InactiveUser" -> forbidden("User account inactive");

                case "NoAccountsAssigned" -> forbidden("User has no assigned accounts");

                default -> unauthorized("Login failed");
            };
        } catch (Exception ex) {

            return unauthorized(
                    "Login failed: "
                            + ex.getMessage()
            );
        }
    }
//////////////////////////////////////////////////////////////
// RESPONSE BUILDERS

    /// ///////////////////////////////////////////////////////////

    private Map<String, Object> buildMobileResponse(
            UserEntity user,
            String jwt
    ) {

        return Map.of(

                "token", jwt,

                "user", Map.of(
                        "id", user.getId(),
                        "name", user.getUserName(),
                        "email", user.getUserEmail(),
                        "image", user.getUserImage(),
                        "appRole",
                        user.getAppRole().name(),
                        "accessRole",
                        user.getAccessRole().name()
                )
        );
    }

//////////////////////////////////////////////////////////////
// HELPERS

    /// ///////////////////////////////////////////////////////////

    private String extractIdToken(
            Map<String, Object> body
    ) {

        Object token = body.get("idToken");

        if (!(token instanceof String)
                || ((String) token).isBlank()) {

            throw new RuntimeException(
                    "Missing idToken"
            );
        }

        return (String) token;
    }

    private ResponseEntity<?> unauthorized(
            String message
    ) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(message);
    }

    private ResponseEntity<?> forbidden(
            String message
    ) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(message);
    }

    //////////////////////////////////////////////////////////////
// MANUAL USER CREATION

    /// ///////////////////////////////////////////////////////////

    @PostMapping("/invite")
    public ResponseEntity<?> inviteUser(
            @RequestBody InviteUserDto request
    ) {
        try {

            // Logged-in user comes from JWT cookie already parsed by your filter
            UUID currentUserId = UserContext.getCurrentUser();

            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }

            UserEntity inviter = userService.getUserById(currentUserId);

            String inviterName =
                    inviter.getUserName() != null
                            ? inviter.getUserName()
                            : inviter.getUserEmail();

            UserEntity user =
                    userService.inviteUser(
                            request.getEmail(),
                            request.getAppRole(),
                            request.getAccessRole(),
                            request.getAccountId(),
                            inviterName
                    );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "Invitation sent successfully",
                            "userId", user.getId(),
                            "email", user.getUserEmail()
                    ));

        } catch (ResponseStatusException ex) {

            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("error", ex.getReason()));

        } catch (IllegalArgumentException ex) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));

        } catch (Exception ex) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send invitation"));
        }
    }

        //-----create demo login for app store
        @PostMapping("/demo-login")
        public ResponseEntity<?> demoLogin () {

            UserEntity demoUser =
                    userRepository
                            .findByUserEmailIgnoreCase("testingtml4@gmail.com")
                            .orElseGet(userService::createDemoUser);

            LoginResponse response =
                    userService.handleOAuthLogin(demoUser);

            return ResponseEntity.ok(response);
        }

    @GetMapping("/me")
    public UserMeResponse me() {

        return userService.getCurrentUser();

    }

    @GetMapping("/history")
    public ResponseEntity<List<UserHistoryEntity>> getAllUserHistory() {
        return ResponseEntity.ok(
                userHistoryRepository.findAllByOrderByChangeAtDesc()
        );
    }

}
