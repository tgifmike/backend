package com.backend.backend.serviceImplementation;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
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
import com.backend.backend.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    private static final String APPLE_REVIEW_EMAIL = "testingtml4@gmail.com";


    public UserServiceImpl(UserRepository userRepository, EmailService emailService, AccountRepository accountRepository, UserAccountAccessRepository userAccountAccessRepository) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.accountRepository = accountRepository;
        this.userAccountAccessRepository = userAccountAccessRepository;
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
        return userRepository.findByUserEmailIgnoreCase(email)
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


//    @Override
//    public UserEntity createOrFindOAuthUser(UserEntity incomingUser) {
//
//        System.out.println("Incoming OAuth user:");
//        System.out.println("email: " + incomingUser.getUserEmail());
//        System.out.println("googleId: " + incomingUser.getGoogleId());
//        System.out.println("appleId: " + incomingUser.getAppleId());
//
//        if (
//                incomingUser.getUserEmail() == null &&
//                        incomingUser.getGoogleId() == null &&
//                        incomingUser.getAppleId() == null
//        ) {
//            throw new IllegalArgumentException("OAuth identity missing");
//        }
//
//        // Normalize email
//        if (incomingUser.getUserEmail() != null) {
//            String normalizedEmail =
//                    incomingUser.getUserEmail().toLowerCase().trim();
//
//            incomingUser.setUserEmail(normalizedEmail);
//        }
//
//        Optional<UserEntity> existingUser = Optional.empty();
//
//        // 1️⃣ provider-based lookup first
//        if (incomingUser.getGoogleId() != null) {
//            existingUser =
//                    userRepository.findByGoogleId(incomingUser.getGoogleId());
//            System.out.println("Lookup by GoogleId: " + existingUser.isPresent());
//        }
//
//        if (existingUser.isEmpty() && incomingUser.getAppleId() != null) {
//            existingUser =
//                    userRepository.findByAppleId(incomingUser.getAppleId());
//            System.out.println("Lookup by AppleId: " + existingUser.isPresent());
//        }
//
//        // 2️⃣ fallback email lookup
//        if (existingUser.isEmpty() && incomingUser.getUserEmail() != null) {
//            existingUser =
//                    userRepository.findByUserEmailIgnoreCase(
//                            incomingUser.getUserEmail().trim()
//                    );
//
//            System.out.println("Lookup by Email: " + existingUser.isPresent());
//        }
//
//        // 3️⃣ user exists → validate invite + update provider IDs
//        if (existingUser.isPresent()) {
//
//            UserEntity user = existingUser.get();
//
//            // do not validate here — handled later
//            // 🚫 block if user disabled
////            if (!user.isUserActive()) {
////                throw new RuntimeException("InactiveUser");
////            }
//
//            // optional (recommended if you add invited column)
////            if (!user.isInvited()) {
////                throw new RuntimeException("AccessDenied");
////            }
//
//            // attach provider IDs if first OAuth login
//            if (incomingUser.getGoogleId() != null) {
//                user.setGoogleId(incomingUser.getGoogleId());
//                user.setProvider("google"); // ← ensure provider is saved
//            }
//            if (incomingUser.getAppleId() != null) {
//                user.setAppleId(incomingUser.getAppleId());
//                user.setProvider("apple"); // ← ensure provider is saved
//            }
//
//            return userRepository.save(user);
//        }
//
//        // 4️⃣ unknown user → reject login (invite-only enforcement)
//        throw new RuntimeException(
//                "User not invited. Please contact your administrator."
//        );
//    }
@Override
public UserEntity createOrFindOAuthUser(UserEntity incomingUser) {
    System.out.println("Incoming OAuth user:");
    System.out.println("email: " + incomingUser.getUserEmail());
    System.out.println("googleId: " + incomingUser.getGoogleId());
    System.out.println("appleId: " + incomingUser.getAppleId());

    // 1️⃣ Basic validation: require at least email or OAuth ID
    if (incomingUser.getUserEmail() == null &&
            incomingUser.getGoogleId() == null &&
            incomingUser.getAppleId() == null) {
        throw new IllegalArgumentException("OAuth identity missing");
    }

    // 2️⃣ Normalize email
    if (incomingUser.getUserEmail() != null) {
        incomingUser.setUserEmail(incomingUser.getUserEmail().toLowerCase().trim());
    }

    Optional<UserEntity> existingUser = Optional.empty();

    // 3️⃣ Primary lookup by email
    if (incomingUser.getUserEmail() != null) {
        existingUser = userRepository.findByUserEmailIgnoreCase(incomingUser.getUserEmail());
        System.out.println("Lookup by Email: " + existingUser.isPresent());
    }

    // 4️⃣ Secondary lookup by provider ID if email not found
    if (existingUser.isEmpty() && incomingUser.getGoogleId() != null) {
        existingUser = userRepository.findByGoogleId(incomingUser.getGoogleId());
        System.out.println("Lookup by GoogleId: " + existingUser.isPresent());
    }
    if (existingUser.isEmpty() && incomingUser.getAppleId() != null) {
        existingUser = userRepository.findByAppleId(incomingUser.getAppleId());
        System.out.println("Lookup by AppleId: " + existingUser.isPresent());
    }

    // 5️⃣ User exists → update provider info and return
    if (existingUser.isPresent()) {
        UserEntity user = existingUser.get();

        // Ensure the user is invited and active
        if (!user.isInvited()) {
            throw new RuntimeException("AccessDenied: User not invited");
        }
        if (!user.isUserActive()) {
            throw new RuntimeException("InactiveUser: User is disabled");
        }

        boolean updated = false;

        // Update provider IDs if missing
        if (incomingUser.getGoogleId() != null && user.getGoogleId() == null) {
            user.setGoogleId(incomingUser.getGoogleId());
            user.setProvider("google");
            updated = true;
        }
        if (incomingUser.getAppleId() != null && user.getAppleId() == null) {
            user.setAppleId(incomingUser.getAppleId());
            user.setProvider("apple");
            updated = true;
        }

        // Update optional fields
        user.setUserName(
                incomingUser.getUserName() != null ? incomingUser.getUserName() : user.getUserName()
        );
        user.setUserImage(
                incomingUser.getUserImage() != null ? incomingUser.getUserImage() : user.getUserImage()
        );

        if (updated) {
            user.setUpdatedAt(Instant.now());
        }

        return userRepository.save(user);
    }

    // 6️⃣ Unknown user → invite-only restriction
    throw new RuntimeException("User not invited. Please contact your administrator.");
}

    /**
     * Universal OAuth login method.
     * Handles Google, Apple, or email-based login.
     * Returns a signed JWT if the user exists and is active.
     */
    @Override
    public String handleOAuthLogin(UserEntity incomingUser) {

        if ((incomingUser.getUserEmail() == null || incomingUser.getUserEmail().isBlank())
                && incomingUser.getGoogleId() == null
                && incomingUser.getAppleId() == null) {

            throw new IllegalArgumentException("OAuth identity missing");
        }

        // Normalize email
        if (incomingUser.getUserEmail() != null) {
            incomingUser.setUserEmail(
                    incomingUser.getUserEmail().toLowerCase().trim()
            );
        }

        Optional<UserEntity> existingUser = Optional.empty();

        // Lookup by provider first
        if (incomingUser.getGoogleId() != null) {
            existingUser = userRepository.findByGoogleId(
                    incomingUser.getGoogleId()
            );
        }

        if (existingUser.isEmpty() && incomingUser.getAppleId() != null) {
            existingUser = userRepository.findByAppleId(
                    incomingUser.getAppleId()
            );
        }

        // fallback email lookup
        if (existingUser.isEmpty()
                && incomingUser.getUserEmail() != null) {

            existingUser =
                    userRepository.findByUserEmailIgnoreCase(
                            incomingUser.getUserEmail()
                    );
        }

    /*
     ----------------------------------------
     APPLE REVIEW BYPASS STARTS HERE
     ----------------------------------------
     */

        boolean isAppleReviewer =
                incomingUser.getUserEmail() != null &&
                        incomingUser.getUserEmail()
                                .equalsIgnoreCase(APPLE_REVIEW_EMAIL);

        if (existingUser.isEmpty()) {

            if (isAppleReviewer) {

                UserEntity reviewer =
                        userRepository.findByUserEmailIgnoreCase(
                                APPLE_REVIEW_EMAIL
                        ).orElseGet(() -> {

                            UserEntity newReviewer =
                                    new UserEntity();

                            newReviewer.setUserEmail(
                                    APPLE_REVIEW_EMAIL
                            );

                            newReviewer.setUserName(
                                    "Apple Reviewer"
                            );

                            newReviewer.setInvited(true);
                            newReviewer.setUserActive(true);

                            return userRepository.save(
                                    newReviewer
                            );
                        });

                return generateJwtForUser(reviewer);
            }

            throw new RuntimeException(
                    "User not invited. Please contact your administrator."
            );
        }

        UserEntity user = existingUser.get();

    /*
     ----------------------------------------
     SKIP ACCESS VALIDATION FOR REVIEWER
     ----------------------------------------
     */

        if (!isAppleReviewer) {
            validateUserAccess(user);
        }

        boolean updated = false;

        if (incomingUser.getGoogleId() != null
                && user.getGoogleId() == null) {

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

        return generateJwtForUser(user);
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
                .withClaim("role", user.getAppRole().name())
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24)) // 24 hours
                .sign(algorithm);
    }


    @Override
    public void validateUserAccess(UserEntity user) {
        if (!user.isInvited()) throw new RuntimeException("AccessDenied");
        if (!user.isUserActive()) throw new RuntimeException("InactiveUser");

        List<AccountEntity> accounts = accountRepository.findAccountsForUser(user.getId());
        System.out.println("User " + user.getUserEmail() + " has accounts: " + accounts.size());

        if (accounts.isEmpty()) {
            throw new RuntimeException("NoAccountsAssigned"); // or handle differently
        }
    }

    //----------------Manager sends email to user to get invited to website/app

    @Override
    public UserEntity inviteUser(
            String email,
            String appRole,
            String accessRole,
            String accountId
    ) {

        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email required");
        }

        String normalizedEmail = email.toLowerCase().trim();

        UserEntity user = userRepository
                .findByUserEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> {
                    UserEntity newUser = new UserEntity();
                    newUser.setUserEmail(normalizedEmail);
                    return newUser;
                });

        user.setInvited(true);
        user.setUserActive(true);
        user.setAccessRole(AccessRole.valueOf(accessRole));
        user.setAppRole(AppRole.valueOf(appRole));

        userRepository.save(user);

        // ⚡ Link user to account
        if (accountId != null && !accountId.isBlank()) {
            UUID accountUuid = UUID.fromString(accountId);
            AccountEntity account = accountRepository.findById(accountUuid)
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            UserAccountAccessEntity access = new UserAccountAccessEntity();
            access.setUser(user);
            access.setAccount(account);

            userAccountAccessRepository.save(access);
        }

        sendInviteEmail(normalizedEmail);

        return user;
    }

    private void sendInviteEmail(String email) {

        String loginUrl =
                "https://www.themanagerlife.com/login?email=" + email;

        String subject =
                "You're invited to The Manager Life";

        String message =
                """
                You've been invited to The Manager Life.
                
                Sign in using Google or Apple with this email:
                
                %s
                
                Login here:
                %s
                """.formatted(email, loginUrl);

        emailService.sendEmail(
                email,
                subject,
                message
        );

    }
}
