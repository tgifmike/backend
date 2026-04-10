package com.backend.backend.controller;

import com.backend.backend.config.GoogleTokenVerifier;
import com.backend.backend.dto.InviteUserDto;
import com.backend.backend.dto.UpdateUserDto;
import com.backend.backend.dto.UserDto;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.repositories.UserRepository;
import com.backend.backend.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.nimbusds.jwt.SignedJWT;

import jakarta.transaction.Transactional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final GoogleTokenVerifier googleTokenVerifier;

    public UserController(
            UserService userService,
            UserRepository userRepository,
            GoogleTokenVerifier googleTokenVerifier
    ) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.googleTokenVerifier = googleTokenVerifier;
    }


//////////////////////////////////////////////////////////////
// ADMIN USER MANAGEMENT ENDPOINTS
    //////////////////////////////////////////////////////////////

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
    //////////////////////////////////////////////////////////////

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
// MOBILE GOOGLE LOGIN
    //////////////////////////////////////////////////////////////

//    @PostMapping("/mobile")
//    public ResponseEntity<?> loginWithGoogle(
//            @RequestBody Map<String, Object> body
//    ) {
//
//        try {
//
//            String idToken = extractIdToken(body);
//
//            GoogleIdToken.Payload payload =
//                    GoogleTokenVerifier.verifyToken(idToken);
//
//            if (payload == null) {
//
//                return unauthorized("Invalid Google token");
//            }
//
//            String googleId = payload.getSubject();
//            String email = payload.getEmail();
//            String name = (String) payload.get("name");
//            String picture = (String) payload.get("picture");
//
//            UserEntity oauthUser = new UserEntity();
//
//            oauthUser.setGoogleId(googleId);
//            oauthUser.setUserEmail(email);
//            oauthUser.setUserName(name);
//            oauthUser.setUserImage(picture);
//
//            return handleOAuthLogin(oauthUser);
//
//        } catch (Exception e) {
//
//            return unauthorized("Google login failed");
//        }
//    }

//////////////////////////////////////////////////////////////
// MOBILE APPLE LOGIN
    //////////////////////////////////////////////////////////////

//    @PostMapping("/mobile/apple")
//    public ResponseEntity<?> loginWithApple(
//            @RequestBody Map<String, Object> body
//    ) {
//
//        try {
//
//            String idToken = extractIdToken(body);
//
//            SignedJWT jwt = SignedJWT.parse(idToken);
//
//            String appleId =
//                    jwt.getJWTClaimsSet().getSubject();
//
//            String email =
//                    (String) jwt.getJWTClaimsSet()
//                            .getClaim("email");
//
//            UserEntity oauthUser = new UserEntity();
//
//            oauthUser.setAppleId(appleId);
//
//            // Apple sometimes only returns email on first login
//            if (email != null) {
//                oauthUser.setUserEmail(email);
//            }
//
//            return handleOAuthLogin(oauthUser);
//
//        } catch (Exception e) {
//
//            return unauthorized("Apple login failed");
//        }
//    }
    //////////////////////////////////////////////////////////////
// UNIVERSAL OAUTH LOGIN ENDPOINT
    ////////////////////////////////////////////////////////////

    @PostMapping("/oauth-login")
    public ResponseEntity<?> loginWithOAuth(@RequestBody Map<String, Object> body) {
        System.out.println("OAUTH LOGIN HIT");

        try {
            String provider = ((String) body.get("provider")).toLowerCase();
            String idToken = extractIdToken(body);

            System.out.println("PROVIDER: " + provider);


            UserEntity oauthUser = new UserEntity();

            switch (provider) {
                case "google" -> {
                    var payload = GoogleTokenVerifier.verifyToken(idToken);
                    if (payload == null) return unauthorized("Invalid Google token");

                    oauthUser.setGoogleId(payload.getSubject());
                    oauthUser.setUserEmail(payload.getEmail());
                    oauthUser.setUserName((String) payload.get("name"));
                    oauthUser.setUserImage((String) payload.get("picture"));
                }
                case "apple" -> {
                    var jwt = SignedJWT.parse(idToken);
                    oauthUser.setAppleId(jwt.getJWTClaimsSet().getSubject());
                    oauthUser.setUserEmail((String) jwt.getJWTClaimsSet().getClaim("email"));
                }
                default -> {
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body("Unsupported provider");
                }



            }

            UserEntity user =
                    userService.createOrFindOAuthUser(oauthUser);

           // user = userService.getUserById(user.getId()); // 🔥 FORCE FRESH ENTITY

            userService.validateUserAccess(user);

            String jwt =
                    userService.generateJwtForUser(user);

            System.out.println("Found user in DB: " + user.getUserEmail() + ", provider=" + user.getProvider() + ", invited=" + user.isInvited() + ", active=" + user.isUserActive());

            System.out.println("DB googleId: " + user.getGoogleId());
            System.out.println("Token googleId: " + oauthUser.getGoogleId());

            return ResponseEntity.ok(buildMobileResponse(user, jwt));

        } catch (RuntimeException ex) {
            String msg = ex.getMessage();
            if ("AccessDenied".equals(msg)) return forbidden("User not invited yet");
            if ("InactiveUser".equals(msg)) return forbidden("User account inactive");
            if ("NoAccountsAssigned".equals(msg))
                return forbidden("User has no assigned accounts");
            return unauthorized("Login failed");
        } catch (Exception ex) {
            System.out.println("LOGIN FAILURE REASON: " + ex.getMessage());
            return unauthorized("Login failed: " + ex.getMessage());
        }
    }



    private ResponseEntity<?> handleOAuthLogin(UserEntity oauthUser) {

        System.out.println("EMAIL FROM WEB LOGIN: " + oauthUser.getUserEmail());

        try {
            // 1️⃣ Find or create user
            UserEntity user = userService.createOrFindOAuthUser(oauthUser);

            // 2️⃣ Validate access
            userService.validateUserAccess(user);

            // 3️⃣ Generate JWT
            String jwt = userService.generateJwtForUser(user);

            // 4️⃣ Build response
            Map<String, Object> response = new HashMap<>();
            response.put("user", Map.of(
                    "id", user.getId(),
                    "email", user.getUserEmail(),
                    "googleId", user.getGoogleId(),
                    "appleId", user.getAppleId(),
                    "accessRole", user.getAccessRole(),
                    "appRole", user.getAppRole(),
                    "invited", user.isInvited(),
                    "userActive", user.isUserActive()
            ));
            response.put("token", jwt);

            return ResponseEntity.ok(response);

        } catch (RuntimeException ex) {
            System.out.println("OAuth login error: " + ex.getMessage());

            if ("AccessDenied".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "User not invited yet"));
            }

            if ("InactiveUser".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "User account inactive"));
            }

            // Return 401 with JSON
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login failed"));
        }
    }

//////////////////////////////////////////////////////////////
// RESPONSE BUILDERS
    //////////////////////////////////////////////////////////////

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
    //////////////////////////////////////////////////////////////

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
    //////////////////////////////////////////////////////////////
    @Transactional
    @PostMapping("/invite")
    public ResponseEntity<?> inviteUser(
            @RequestBody InviteUserDto request
    ) {

        try {

            UserEntity user =
                    userService.inviteUser(
                            request.getEmail(),
                            request.getAppRole(),
                            request.getAccessRole(),
                            request.getAccountId()
                    );

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Invitation sent",
                            "user", user
                    )
            );

        }

        catch (RuntimeException ex) {

            return ResponseEntity
                    .badRequest()
                    .body(ex.getMessage());

        }
    }
}

