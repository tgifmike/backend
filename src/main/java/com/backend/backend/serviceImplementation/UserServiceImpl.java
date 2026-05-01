package com.backend.backend.serviceImplementation;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.backend.backend.config.UserContext;
import com.backend.backend.dto.LoginResponse;
import com.backend.backend.dto.UserDto;
import com.backend.backend.dto.UserMeResponse;
import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.UserAccountAccessEntity;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.entity.UserHistoryEntity;
import com.backend.backend.enums.AccessRole;
import com.backend.backend.enums.AppRole;
import com.backend.backend.enums.HistoryType;
import com.backend.backend.repositories.AccountRepository;
import com.backend.backend.repositories.UserAccountAccessRepository;
import com.backend.backend.repositories.UserHistoryRepository;
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
import java.util.*;

@Service
public class UserServiceImpl implements UserService {

    @Value("${jwt.secret}")
    private String secret;

    private static final String APPLE_REVIEW_EMAIL = "testingtml4@gmail.com";

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AccountRepository accountRepository;
    private final UserAccountAccessRepository userAccountAccessRepository;
    private final EmailTemplateService emailTemplateService;
    private final UserHistoryRepository userHistoryRepository;

    public UserServiceImpl(
            UserRepository userRepository,
            EmailService emailService,
            AccountRepository accountRepository,
            UserAccountAccessRepository userAccountAccessRepository,
            EmailTemplateService emailTemplateService,
            UserHistoryRepository userHistoryRepository
    ) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.accountRepository = accountRepository;
        this.userAccountAccessRepository = userAccountAccessRepository;
        this.emailTemplateService = emailTemplateService;
        this.userHistoryRepository = userHistoryRepository;
    }

    // =====================================================
    // CREATE USER
    // =====================================================

    @Override
    @Transactional
    public UserEntity createUser(UserEntity user) {

        validateCreateUser(user);

        normalizeEmail(user);

        user.setUserName(user.getUserName().trim());

        if (user.getAccessRole() == null) {
            user.setAccessRole(AccessRole.USER);
        }

        if (user.getAppRole() == null) {
            user.setAppRole(AppRole.MEMBER);
        }

        user.setUserActive(true);
        user.setFirstLogin(true);
        user.setInvited(true);

        UserEntity saved = userRepository.save(user);

        logHistory(saved, HistoryType.CREATED, new HashMap<>());

        return saved;
    }

    // =====================================================
    // READ
    // =====================================================

    @Override
    public List<UserEntity> getAllUsers() {
        return userRepository.findAllByDeletedAtIsNull();
    }

    @Override
    public UserEntity getUserById(UUID id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public UserEntity findByEmail(String email) {
        return userRepository.findByUserEmailIgnoreCaseAndDeletedAtIsNull(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public UserMeResponse getCurrentUser() {

        UUID userId = UserContext.getCurrentUser();

        if (userId == null) {
            throw new RuntimeException("Missing authentication context");
        }

        UserEntity user = getUserById(userId);

        return UserMeResponse.builder()
                .id(user.getId().toString())
                .name(user.getUserName())
                .email(user.getUserEmail())
                .image(user.getUserImage())
                .appRole(user.getAppRole().name())
                .accessRole(user.getAccessRole().name())
                .build();
    }

    // =====================================================
    // UPDATE GENERIC
    // =====================================================

    @Override
    @Transactional
    public UserEntity updateUser(UserEntity user) {
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUser(UUID id, String newName, String newEmail) {

        UserEntity user = getUserById(id);

        Map<String, String> oldValues = new HashMap<>();

        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("User name cannot be blank");
        }

        if (newEmail == null || newEmail.isBlank()) {
            throw new IllegalArgumentException("User email cannot be blank");
        }

        newEmail = newEmail.trim().toLowerCase();
        newName = newName.trim();

        if (isNameDuplicate(newName, id)) {
            throw new IllegalArgumentException("User name already exists");
        }

        if (isEmailDuplicate(newEmail, id)) {
            throw new IllegalArgumentException("User email already exists");
        }

        if (!Objects.equals(user.getUserName(), newName)) {
            oldValues.put("userName", user.getUserName());
            user.setUserName(newName);
        }

        if (!Objects.equals(user.getUserEmail(), newEmail)) {
            oldValues.put("userEmail", user.getUserEmail());
            user.setUserEmail(newEmail);
        }

        userRepository.save(user);

        if (!oldValues.isEmpty()) {
            logHistory(user, HistoryType.UPDATED, oldValues);
        }
    }

    // =====================================================
    // DELETE
    // =====================================================

    @Override
    @Transactional
    public void deleteUser(UUID id) {

        UserEntity user = getUserById(id);

        user.setDeletedAt(Instant.now());
        user.setDeletedBy(currentUserId());
        user.setUserActive(false);
        user.setGoogleId(null);
        user.setAppleId(null);

        userRepository.save(user);

        logHistory(user, HistoryType.DELETED, new HashMap<>());
    }

    // =====================================================
    // ACTIVE TOGGLE
    // =====================================================

    @Override
    @Transactional
    public UserDto toggleActive(UUID id, boolean active) {

        UserEntity user = getUserById(id);

        Map<String, String> oldValues = Map.of(
                "userActive",
                String.valueOf(user.isUserActive())
        );

        user.setUserActive(active);

        UserEntity saved = userRepository.save(user);

        logHistory(
                saved,
                active ? HistoryType.ACTIVATED : HistoryType.DEACTIVATED,
                oldValues
        );

        return toDto(saved);
    }

    // =====================================================
    // ROLE CHANGES
    // =====================================================

    @Override
    @Transactional
    public UserDto updateAccessRole(UUID id, String role) {

        UserEntity user = getUserById(id);

        Map<String, String> oldValues = Map.of(
                "accessRole",
                user.getAccessRole().name()
        );

        user.setAccessRole(AccessRole.valueOf(role.toUpperCase()));

        UserEntity saved = userRepository.save(user);

        logHistory(saved, HistoryType.ROLE_CHANGED, oldValues);

        return toDto(saved);
    }

    @Override
    @Transactional
    public UserDto updateAppRole(UUID id, String role) {

        UserEntity user = getUserById(id);

        Map<String, String> oldValues = Map.of(
                "appRole",
                user.getAppRole().name()
        );

        user.setAppRole(AppRole.valueOf(role.toUpperCase()));

        UserEntity saved = userRepository.save(user);

        logHistory(saved, HistoryType.ROLE_CHANGED, oldValues);

        return toDto(saved);
    }

    // =====================================================
    // DUPLICATES
    // =====================================================

    @Override
    public boolean isEmailDuplicate(String email, UUID excludeId) {
        return userRepository.existsByUserEmailAndIdNotAndDeletedAtIsNull(email, excludeId);
    }

    @Override
    public boolean isNameDuplicate(String name, UUID excludeId) {
        return userRepository.existsByUserNameAndIdNotAndDeletedAtIsNull(name, excludeId);
    }

    // =====================================================
    // OAUTH LOGIN
    // =====================================================

    @Override
    @Transactional
    public LoginResponse handleOAuthLogin(UserEntity incomingUser) {

        normalizeEmail(incomingUser);

        UserEntity user = resolveUserIdentity(incomingUser);

        validateUserStatus(user);

        boolean updated = false;

        if (linkProvider(user, incomingUser)) {
            updated = true;
        }

        if ((user.getUserName() == null || user.getUserName().isBlank())
                && incomingUser.getUserName() != null) {
            user.setUserName(incomingUser.getUserName().trim());
            updated = true;
        }

        if ((user.getUserImage() == null || user.getUserImage().isBlank())
                && incomingUser.getUserImage() != null) {
            user.setUserImage(incomingUser.getUserImage());
            updated = true;
        }

        if (user.isFirstLogin()) {
            user.setFirstLogin(false);
            updated = true;
        }

        if (updated) {
            user = userRepository.save(user);
        }

        boolean hasAccess =
                accountRepository.countAccounts(user.getId()) > 0
                        || isSystemUser(user);

        return new LoginResponse(
                generateJwtForUser(user),
                user.getId(),
                user.getUserEmail(),
                user.getUserName(),
                user.getAppRole().name(),
                user.getAccessRole().name(),
                hasAccess,
                user.getUserImage()
        );
    }

    @Override
    public UserEntity resolveUserIdentity(UserEntity incomingUser) {

        Optional<UserEntity> userOpt = Optional.empty();

        if (incomingUser.getGoogleId() != null) {
            userOpt = userRepository.findByGoogleIdAndDeletedAtIsNull(
                    incomingUser.getGoogleId()
            );
        }

        if (userOpt.isEmpty() && incomingUser.getAppleId() != null) {
            userOpt = userRepository.findByAppleIdAndDeletedAtIsNull(
                    incomingUser.getAppleId()
            );
        }

        if (userOpt.isEmpty() && incomingUser.getUserEmail() != null) {
            userOpt = userRepository.findByUserEmailIgnoreCaseAndDeletedAtIsNull(
                    incomingUser.getUserEmail()
            );
        }

        if (userOpt.isEmpty()) {

            if (isSystemUser(incomingUser)) {
                return createDemoUser();
            }

            throw new RuntimeException(
                    "User not invited. Please contact your administrator."
            );
        }

        return userOpt.get();
    }

    // =====================================================
    // INVITE USER
    // =====================================================

    @Override
    @Transactional
    public UserEntity inviteUser(
            String email,
            String appRole,
            String accessRole,
            String accountId,
            String inviterName
    ) {

        String normalizedEmail = email.trim().toLowerCase();

        AccessRole parsedAccess = AccessRole.valueOf(accessRole.toUpperCase());
        AppRole parsedApp = AppRole.valueOf(appRole.toUpperCase());

        UserEntity user = userRepository
                .findByUserEmailIgnoreCase(normalizedEmail)
                .orElse(null);

        boolean restored = false;

        if (user == null) {
            user = new UserEntity();
            user.setUserEmail(normalizedEmail);
        } else if (user.getDeletedAt() != null) {
            restored = true;
            user.setDeletedAt(null);
        }

        user.setInvited(true);
        user.setUserActive(true);
        user.setAccessRole(parsedAccess);
        user.setAppRole(parsedApp);

        user = userRepository.save(user);

        resolveAccountAccessIfPresent(accountId, user);

        sendInviteEmail(
                normalizedEmail,
                inviterName,
                "Your Organization",
                parsedApp.name(),
                parsedAccess.name()
        );

        logHistory(
                user,
                restored ? HistoryType.RESTORED : HistoryType.INVITED,
                new HashMap<>()
        );

        return user;
    }

    // =====================================================
    // JWT
    // =====================================================

    @Override
    public String generateJwtForUser(UserEntity user) {

        Algorithm algorithm = Algorithm.HMAC256(secret);

        return JWT.create()
                .withSubject(user.getId().toString())
                .withClaim("email", user.getUserEmail())
                .withClaim("name", user.getUserName())
                .withClaim("accessRole", user.getAccessRole().name())
                .withClaim("appRole", user.getAppRole().name())
                .withClaim("role", user.getAppRole().name())
                .withClaim("mode", isSystemUser(user) ? "demo" : "normal")
                .withExpiresAt(
                        new Date(System.currentTimeMillis() + 86400000)
                )
                .sign(algorithm);
    }

    // =====================================================
    // DEMO USER
    // =====================================================

    @Override
    public UserEntity createDemoUser() {

        return userRepository
                .findByUserEmailIgnoreCase(APPLE_REVIEW_EMAIL)
                .orElseGet(() -> {

                    UserEntity user = new UserEntity();

                    user.setUserEmail(APPLE_REVIEW_EMAIL);
                    user.setUserName("Apple Reviewer");
                    user.setInvited(true);
                    user.setUserActive(true);
                    user.setAccessRole(AccessRole.ADMIN);
                    user.setAppRole(AppRole.MANAGER);

                    return userRepository.save(user);
                });
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private void validateCreateUser(UserEntity user) {

        if (user.getUserName() == null || user.getUserName().isBlank()) {
            throw new RuntimeException("Name cannot be empty");
        }

        if (user.getUserEmail() == null || user.getUserEmail().isBlank()) {
            throw new RuntimeException("Email cannot be empty");
        }

        if (userRepository.existsByUserEmailAndDeletedAtIsNull(user.getUserEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (userRepository.existsByUserNameAndDeletedAtIsNull(user.getUserName())) {
            throw new RuntimeException("User name already exists");
        }
    }

    private void validateUserStatus(UserEntity user) {

        if (isSystemUser(user)) {
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

    private boolean linkProvider(UserEntity user, UserEntity incoming) {

        boolean updated = false;

        if (incoming.getGoogleId() != null && user.getGoogleId() == null) {
            user.setGoogleId(incoming.getGoogleId());
            user.setProvider("google");
            updated = true;
        }

        if (incoming.getAppleId() != null && user.getAppleId() == null) {
            user.setAppleId(incoming.getAppleId());
            user.setProvider("apple");
            updated = true;
        }

        if (updated) {
            logHistory(user, HistoryType.LOGIN_PROVIDER_LINKED, new HashMap<>());
        }

        return updated;
    }

    private void normalizeEmail(UserEntity user) {
        if (user.getUserEmail() != null) {
            user.setUserEmail(
                    user.getUserEmail().trim().toLowerCase()
            );
        }
    }

    private boolean isSystemUser(UserEntity user) {
        return user.getUserEmail() != null
                && user.getUserEmail().equalsIgnoreCase(APPLE_REVIEW_EMAIL);
    }

    private UserDto toDto(UserEntity user) {

        return UserDto.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .userEmail(user.getUserEmail())
                .userImage(user.getUserImage())
                .userActive(user.isUserActive())
                .accessRole(user.getAccessRole().name())
                .appRole(user.getAppRole().name())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private UUID currentUserId() {
        return UserContext.getCurrentUser() != null
                ? UserContext.getCurrentUser()
                : UUID.randomUUID();
    }

    private String currentUserName() {

        UUID id = UserContext.getCurrentUser();

        if (id == null) {
            return "System";
        }

        return userRepository.findById(id)
                .map(UserEntity::getUserName)
                .orElse("System");
    }

    private void logHistory(
            UserEntity user,
            HistoryType type,
            Map<String, String> oldValues
    ) {

        userHistoryRepository.save(
                UserHistoryEntity.builder()
                        .userId(user.getId())
                        .userName(user.getUserName())
                        .userEmail(user.getUserEmail())
                        .userActive(user.isUserActive())
                        .accessRole(user.getAccessRole().name())
                        .appRole(user.getAppRole().name())
                        .changeType(type)
                        .changeAt(Instant.now())
                        .changedBy(currentUserId())
                        .changedByName(currentUserName())
                        .oldValues(oldValues)
                        .build()
        );
    }

    private void resolveAccountAccessIfPresent(
            String accountId,
            UserEntity user
    ) {

        if (accountId == null || accountId.isBlank()) {
            return;
        }

        UUID uuid = UUID.fromString(accountId);

        AccountEntity account = accountRepository.findById(uuid)
                .orElseThrow(() ->
                        new RuntimeException("Account not found")
                );

        if (!userAccountAccessRepository.existsByUserIdAndAccountId(
                user.getId(),
                uuid
        )) {

            UserAccountAccessEntity access =
                    new UserAccountAccessEntity();

            access.setUser(user);
            access.setAccount(account);

            userAccountAccessRepository.save(access);
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

        String html = emailTemplateService.buildInviteEmail(
                inviterName,
                accountName,
                appRole,
                accessRole,
                email,
                loginUrl
        );

        String text = """
                %s invited you to join The Manager Life.

                Account: %s
                App Role: %s
                Access Role: %s

                Sign in:
                %s
                """
                .formatted(
                        inviterName,
                        accountName,
                        appRole,
                        accessRole,
                        loginUrl
                );

        emailService.sendMultipartEmail(
                email,
                subject,
                text,
                html
        );
    }
}