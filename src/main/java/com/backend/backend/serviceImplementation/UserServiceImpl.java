package com.backend.backend.serviceImplementation;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.backend.backend.config.UserContext;
import com.backend.backend.dto.LoginResponse;
import com.backend.backend.dto.UserMeResponse;
import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.UserAccountAccessEntity;
import com.backend.backend.enums.AccessRole;
import com.backend.backend.enums.AppRole;
import com.backend.backend.dto.UserDto;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.repositories.AccountRepository;
import com.backend.backend.repositories.UserAccountAccessRepository;
import com.backend.backend.repositories.UserRepository;
import com.backend.backend.service.EmailService;
import com.backend.backend.service.EmailTemplateService;
import com.backend.backend.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {


    @Value("${jwt.secret}")
    private String secret;

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AccountRepository accountRepository;
    private final UserAccountAccessRepository userAccountAccessRepository;
    private final EmailTemplateService emailTemplateService;

    private static final String APPLE_REVIEW_EMAIL = "testingtml4@gmail.com";


    public UserServiceImpl(UserRepository userRepository,
                           EmailService emailService,
                           AccountRepository accountRepository,
                           UserAccountAccessRepository userAccountAccessRepository,
                           EmailTemplateService emailTemplateService) {

        this.userRepository = userRepository;
        this.emailService = emailService;
        this.accountRepository = accountRepository;
        this.userAccountAccessRepository = userAccountAccessRepository;
        this.emailTemplateService = emailTemplateService;
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
    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    //find by id
    @Override
    public UserEntity getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id " + id));
    }

    //update user name and email
    @Override
    public UserEntity updateUser(UserEntity user) {
        return userRepository.save(user);
    }

    //find by email
    @Override
    public UserEntity findByEmail(String email) {
        return userRepository.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found with email " + email));
    }


    //delete user soft
    @Override
    @Transactional
    public void deleteUser(UUID id) {

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
                );

        user.setDeletedAt(Instant.now());
        user.setUserActive(false);

        userRepository.save(user);
    }

    //toggle active
    @Override
    @Transactional
    public UserDto toggleActive(UUID id, boolean active) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        user.setUserActive(active);
        user.setUpdatedAt(Instant.now());

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
    public UserDto updateAppRole(UUID id, String role) {
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
    public void updateUser(UUID id, String newName, String newEmail) {
        //validate input
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be blank");
        }
        if (newEmail == null || newEmail.trim().isEmpty()) {
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

        if (userOpt.isPresent()) {
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
    public UserEntity resolveUserIdentity(UserEntity incomingUser) {


        Optional<UserEntity> userOpt = Optional.empty();

        if (incomingUser.getGoogleId() != null) {

            userOpt =
                    userRepository.findByGoogleId(
                            incomingUser.getGoogleId()
                    );

//            System.out.println("Lookup by GoogleId: " + userOpt.isPresent());
        }

        if (userOpt.isEmpty()
                && incomingUser.getAppleId() != null) {

            userOpt =
                    userRepository.findByAppleId(
                            incomingUser.getAppleId()
                    );

//            System.out.println("Lookup by AppleId: " + userOpt.isPresent());
        }

        if (userOpt.isEmpty()
                && incomingUser.getUserEmail() != null) {

            userOpt =
                    userRepository.findByUserEmailIgnoreCase(
                            incomingUser.getUserEmail()
                    );

//            System.out.println("Lookup by Email: " + userOpt.isPresent());
        }

        if (userOpt.isEmpty()) {

//            System.out.println("USER NOT FOUND IN DB");

            if (isAppleReviewer(incomingUser)) {

                return createAppleReviewerUser();
            }

            throw new RuntimeException(
                    "User not invited. Please contact your administrator."
            );
        }

        UserEntity resolvedUser = userOpt.get();


        return resolvedUser;
    }

    /**
     * Universal OAuth login method.
     * Handles Google, Apple, or email-based login.
     * Returns a signed JWT if the user exists and is active.
     */
    @Transactional
    @Override
    public LoginResponse handleOAuthLogin(UserEntity incomingUser) {

        normalizeEmail(incomingUser);

        UserEntity user = resolveUserIdentity(incomingUser);

        validateUserStatus(user);

        updateProviderIdsIfNeeded(user, incomingUser);

        long accountCount =
                accountRepository.countAccounts(user.getId());


        boolean hasAccess =
                accountCount > 0
                        || isDemoUser(user);

        String jwt = generateJwtForUser(user);

        return new LoginResponse(
                jwt,
                user.getId(),
                user.getUserEmail(),
                user.getUserName(),
                user.getAppRole().name(),
                user.getAccessRole().name(),
                hasAccess,
                user.getUserImage()

        );
    }

    private void validateUserStatus(UserEntity user) {

        if (isDemoUser(user)) {
            return;
        }

        if (user.getDeletedAt() != null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Account has been deleted"
            );
        }

        if (!user.isInvited()) {

            throw new RuntimeException("AccessDenied");
        }

        if (!user.isUserActive()) {

            throw new RuntimeException("InactiveUser");
        }
    }

    private void updateProviderIdsIfNeeded(
            UserEntity user,
            UserEntity incomingUser
    ) {

        boolean updated = false;


        if (incomingUser.getGoogleId() != null
                && user.getGoogleId() == null
                && incomingUser.getUserEmail() != null
                && incomingUser.getUserEmail()
                .equalsIgnoreCase(user.getUserEmail())) {

            user.setGoogleId(incomingUser.getGoogleId());
            user.setProvider("google");

            updated = true;
        }

        if (incomingUser.getAppleId() != null
                && user.getAppleId() == null) {

            user.setAppleId(incomingUser.getAppleId());
            user.setProvider("apple");

            updated = true;
        }

        if (updated) {

            user.setUpdatedAt(Instant.now());

            userRepository.save(user);
        }


    }

    private void normalizeEmail(UserEntity user) {

        if (user.getUserEmail() != null) {

            user.setUserEmail(
                    user.getUserEmail()
                            .toLowerCase()
                            .trim()
            );
        }
    }

    private boolean isAppleReviewer(UserEntity user) {

        return user.getUserEmail() != null
                && user.getUserEmail()
                .equalsIgnoreCase(APPLE_REVIEW_EMAIL);
    }

    private UserEntity createAppleReviewerUser() {

        return userRepository
                .findByUserEmailIgnoreCase(APPLE_REVIEW_EMAIL)
                .orElseGet(() -> {

                    UserEntity reviewer =
                            new UserEntity();

                    reviewer.setUserEmail(APPLE_REVIEW_EMAIL);

                    reviewer.setUserName("Apple Reviewer");

                    reviewer.setInvited(true);

                    reviewer.setUserActive(true);

                    return userRepository.save(reviewer);
                });
    }


    @Override
    public String generateJwtForUser(UserEntity user) {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("JWT secret is not set!");
        }
        Algorithm algorithm = Algorithm.HMAC256(secret);

        return JWT.create()
                .withSubject(user.getId().toString())
                .withClaim("email", user.getUserEmail())
                .withClaim("name", user.getUserName())
                .withClaim("role",
                        user.getAppRole() != null
                                ? user.getAppRole().name()
                                : AppRole.MEMBER.name()
                )
                .withClaim(
                        "mode",
                        isDemoUser(user) ? "demo" : "normal"
                )
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24))
                .sign(algorithm);
    }

    //----------------Create demo user
    public UserEntity createDemoUser() {

        UserEntity demo = new UserEntity();

        demo.setUserEmail("testingtml4@gmail.com");
        demo.setUserName("Demo User");
        demo.setInvited(true);
        demo.setUserActive(true);

        demo.setAppRole(AppRole.MANAGER);
        demo.setAccessRole(AccessRole.ADMIN);

        return userRepository.save(demo);
    }


    //----------------Manager sends email to user to get invited to website/app




    @Override
    @Transactional
    public UserEntity inviteUser(
            String email,
            String appRole,
            String accessRole,
            String accountId,
            String inviterName
    ) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (inviterName == null || inviterName.isBlank()) {
            throw new IllegalArgumentException("Inviter name required");
        }


        String normalizedEmail =
                email.trim().toLowerCase();


        AccessRole parsedAccessRole =
                parseAccessRole(accessRole);

        AppRole parsedAppRole =
                parseAppRole(appRole);


        UserEntity user =
                userRepository
                        .findByUserEmailIgnoreCase(normalizedEmail)
                        .orElseGet(() -> {

                            UserEntity newUser =
                                    new UserEntity();

                            newUser.setUserEmail(normalizedEmail);

                            return newUser;
                        });


        user.setInvited(true);
        user.setUserActive(true);
        user.setAccessRole(parsedAccessRole);
        user.setAppRole(parsedAppRole);


        userRepository.save(user);


        String accountName =
                resolveAccountAccessIfPresent(
                        accountId,
                        user
                );


        sendInviteEmail(
                normalizedEmail,
                inviterName,
                accountName,
                parsedAppRole.name(),
                parsedAccessRole.name()
        );


        return user;
    }

    private String resolveAccountAccessIfPresent(
            String accountId,
            UserEntity user
    ) {

        if (accountId == null || accountId.isBlank()) {
            return "Your Organization";
        }

        UUID accountUuid =
                UUID.fromString(accountId);

        AccountEntity account =
                accountRepository
                        .findById(accountUuid)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Account not found"
                                )
                        );


        boolean alreadyLinked =
                userAccountAccessRepository
                        .existsByUserIdAndAccountId(
                                user.getId(),
                                accountUuid
                        );


        if (!alreadyLinked) {

            UserAccountAccessEntity access =
                    new UserAccountAccessEntity();

            access.setUser(user);
            access.setAccount(account);

            userAccountAccessRepository.save(access);
        }


        return account.getAccountName();
    }

    private AccessRole parseAccessRole(String role) {

        try {

            return AccessRole.valueOf(
                    role.toUpperCase()
            );

        } catch (Exception ex) {

            throw new IllegalArgumentException(
                    "Invalid access role: " + role
            );
        }
    }

    private AppRole parseAppRole(String role) {

        try {

            return AppRole.valueOf(
                    role.toUpperCase()
            );

        } catch (Exception ex) {

            throw new IllegalArgumentException(
                    "Invalid app role: " + role
            );
        }
    }

    private void sendInviteEmail(
            String email,
            String inviterName,
            String accountName,
            String appRole,
            String accessRole
    ) {

        String loginUrl =
                "https://www.themanagerlife.com/login?email=" + email;

        String subject =
                inviterName + " invited you to join The Manager Life";

        try {
            System.out.println("ABOUT TO BUILD TEMPLATE");

            String htmlMessage = emailTemplateService.buildInviteEmail(
                    inviterName,
                    accountName,
                    appRole,
                    accessRole,
                    email,
                    loginUrl
            );

            System.out.println("TEMPLATE OK");

            String plainTextMessage = buildInviteText(
                    inviterName,
                    accountName,
                    appRole,
                    accessRole,
                    loginUrl,
                    email
            );

            System.out.println("ABOUT TO SEND EMAIL");

            emailService.sendMultipartEmail(
                    email,
                    subject,
                    plainTextMessage,
                    htmlMessage
            );

            System.out.println("EMAIL SENT CALL FINISHED");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Email failed: " + e.getMessage(), e);
        }
    }

    private String buildInviteText(
            String inviterName,
            String accountName,
            String appRole,
            String accessRole,
            String loginUrl,
            String email
    ) {

        return """
                %s invited you to join The Manager Life.
                
                Account: %s
                
                App Role: %s
                Access Role: %s
                
                Sign in:
                %s
                
                Email:
                %s
                
                Invitation expires in 7 days.
                """
                .formatted(
                        inviterName,
                        accountName,
                        appRole,
                        accessRole,
                        loginUrl,
                        email
                );
    }

    private boolean isDemoUser(UserEntity user) {

        if (user == null || user.getUserEmail() == null) {
            return false;
        }

        return user.getUserEmail()
                .equalsIgnoreCase(APPLE_REVIEW_EMAIL);
    }

    @Override
    public UserMeResponse getCurrentUser() {

        UUID userId = UserContext.getCurrentUser();

        if (userId == null) {
            System.out.println("❌ UserContext is NULL in /users/me");
            throw new RuntimeException("Missing authentication context");
        }

        UserEntity user = userRepository
                .findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserMeResponse.builder()
                .id(user.getId().toString())
                .name(user.getUserName())
                .email(user.getUserEmail())
                .image(user.getUserImage())
                .appRole(user.getAppRole().name())
                .accessRole(user.getAccessRole().name())
                .build();
    }

}