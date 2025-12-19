package com.backend.backend.serviceImplementation;

import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.UserAccountAccessEntity;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.repositories.UserAccountAccessRepository;
import com.backend.backend.service.UserAccountAccessService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserAccountAccessServiceImpl implements UserAccountAccessService {

    private final UserAccountAccessRepository userAccountAccessRepository;


    public UserAccountAccessServiceImpl(UserAccountAccessRepository userAccountAccessRepository) {
        this.userAccountAccessRepository = userAccountAccessRepository;
    }

    @Override
    public List<AccountEntity> getAccountsForUser(UserEntity user) {
        // Use a custom query in the repository to fetch only active accounts
        return userAccountAccessRepository.findActiveAccountsByUserId(user.getId());
    }


    @Override
    public List<UserAccountAccessEntity> getUsersForAccount(AccountEntity account) {
        return userAccountAccessRepository.findByAccount(account);
    }

    @Override
    public UserAccountAccessEntity grantAccess(UserEntity user, AccountEntity account) {
        if (userAccountAccessRepository.existsByUserAndAccount(user, account)) {
            return null; // Already has access
        }
        UserAccountAccessEntity access = new UserAccountAccessEntity();
        access.setUser(user);
        access.setAccount(account);
        return userAccountAccessRepository.save(access);
    }

    @Override
    public void revokeAccess(UserEntity user, AccountEntity account) {
        List<UserAccountAccessEntity> accesses = userAccountAccessRepository.findByUser(user);
        accesses.stream()
                .filter(a -> a.getAccount().getId().equals(account.getId()))
                .forEach(userAccountAccessRepository::delete);
    }

    @Override
    public boolean userHasAccessToAccount(UUID userId, UUID accountId) {
        return userAccountAccessRepository.existsByUserIdAndAccountId(userId, accountId);
    }
}
