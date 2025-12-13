package com.backend.backend.serviceImplementation;

import com.backend.backend.service.OptionService;
import com.backend.backend.enums.OptionType;
import com.backend.backend.dto.OptionCreateDto;
import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.OptionEntity;
import com.backend.backend.entity.OptionHistoryEntity;
import com.backend.backend.entity.OptionHistoryEntity.ChangeType;
import com.backend.backend.repositories.AccountRepository;
import com.backend.backend.repositories.OptionRepository;
import com.backend.backend.repositories.OptionHistoryRepository;
import com.backend.backend.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OptionServiceImpl implements OptionService {

    private final OptionRepository optionRepository;
    private final AccountRepository accountRepository;
    private final OptionHistoryRepository optionHistoryRepository;
    private final UserRepository userRepository;

    // ------------------- Helper -------------------
    private void recordHistory(
            OptionEntity option,
            UUID changedBy,
            ChangeType changeType,
            Map<String, Object> oldValues
    ) {
        String changedByName = getUserNameById(changedBy);

        OptionHistoryEntity history = OptionHistoryEntity.builder()
                .optionId(option.getId())
                .accountId(option.getAccount().getId())
                .optionName(option.getOptionName())
                .optionActive(option.isOptionActive())
                .optionType(option.getOptionType())
                .sortOrder(option.getSortOrder())
                .changeType(changeType)
                .changedBy(changedBy)
                .changedByName(changedByName)  // ✅ actual name
                .changeAt(Instant.now())
                .oldValues(oldValues != null
                        ? oldValues.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()))
                        : new HashMap<>())
                .build();

        optionHistoryRepository.save(history);
    }

    private String getUserNameById(UUID userId) {
        if (userId == null) return "System";
        return userRepository.findById(userId)
                .map(u -> u.getUserName() != null ? u.getUserName() : u.getId().toString())
                .orElse("Unknown User");
    }

    // ------------------- GETTERS -------------------
    @Override
    public List<OptionEntity> getAllOptions(UUID accountId) {
        return optionRepository.findByAccountIdOrderBySortOrderAsc(accountId);
    }

    @Override
    public List<OptionEntity> getOptionsByType(UUID accountId, OptionType optionType) {
        return optionRepository.findByAccountIdAndOptionTypeOrderBySortOrderAsc(accountId, optionType);
    }

    @Override
    public List<OptionEntity> getOptionsByAccount(UUID accountId) {
        return optionRepository.findByAccountIdOrderBySortOrderAsc(accountId);
    }

    // ------------------- CREATE -------------------
    @Override
    @Transactional
    public OptionEntity createOption(OptionCreateDto dto, UUID userId) {
        AccountEntity account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account not found"));

        OptionEntity option = OptionEntity.builder()
                .optionName(dto.getOptionName())
                .optionActive(dto.getOptionActive() != null && dto.getOptionActive())
                .optionType(dto.getOptionType())
                .account(account)
                .createdBy(userId)
                .build();

        OptionEntity saved = optionRepository.save(option);

        // Record history
        recordHistory(saved, userId, ChangeType.CREATED, null);

        return saved;
    }

    // ------------------- UPDATE -------------------
    @Override
    @Transactional
    public OptionEntity updateOption(UUID optionId, OptionEntity updated, UUID userId) {
        OptionEntity existing = optionRepository.findById(optionId)
                .orElseThrow(() -> new NoSuchElementException("Option not found"));

        Map<String, Object> oldValues = new HashMap<>();
        if (!Objects.equals(existing.getOptionName(), updated.getOptionName()))
            oldValues.put("optionName", existing.getOptionName());
        if (!Objects.equals(existing.getOptionType(), updated.getOptionType()))
            oldValues.put("optionType", existing.getOptionType());
        if (!Objects.equals(existing.getSortOrder(), updated.getSortOrder()))
            oldValues.put("sortOrder", existing.getSortOrder());
        if (existing.isOptionActive() != updated.isOptionActive())
            oldValues.put("optionActive", existing.isOptionActive());

        existing.setOptionName(updated.getOptionName());
        existing.setOptionType(updated.getOptionType());
        existing.setSortOrder(updated.getSortOrder());
        existing.setOptionActive(updated.isOptionActive());
        existing.setUpdatedBy(userId);
        existing.setUpdatedAt(Instant.now());

        OptionEntity saved = optionRepository.save(existing);

        if (!oldValues.isEmpty()) {
            recordHistory(saved, userId, ChangeType.UPDATED, oldValues);
        }

        return saved;
    }

    // ------------------- DELETE -------------------
    @Override
    @Transactional
    public void deleteOption(UUID optionId, UUID deletedByUser) {
        OptionEntity option = optionRepository.findById(optionId)
                .orElseThrow(() -> new NoSuchElementException("Option not found"));

        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("optionName", option.getOptionName());
        oldValues.put("optionType", option.getOptionType());
        oldValues.put("sortOrder", option.getSortOrder());
        oldValues.put("optionActive", option.isOptionActive());

        option.setDeletedAt(Instant.now());
        option.setDeletedBy(deletedByUser);
        option.setUpdatedAt(Instant.now());

        optionRepository.saveAndFlush(option);

        recordHistory(option, deletedByUser, ChangeType.DELETED, oldValues);
    }

    // ------------------- REORDER -------------------
    @Override
    @Transactional
    public void reorderOptions(UUID accountId, OptionType optionType, List<UUID> orderedIds, UUID userId) {
        List<OptionEntity> options = optionType != null
                ? optionRepository.findByAccountIdAndOptionTypeOrderBySortOrderAsc(accountId, optionType)
                : optionRepository.findByAccountIdOrderBySortOrderAsc(accountId);

        Map<UUID, OptionEntity> map = options.stream()
                .collect(Collectors.toMap(OptionEntity::getId, o -> o));

        for (int i = 0; i < orderedIds.size(); i++) {
            OptionEntity opt = map.get(orderedIds.get(i));
            if (opt != null && !Objects.equals(opt.getSortOrder(), i)) {
                Map<String, Object> oldValues = new HashMap<>();
                if (opt.getSortOrder() != null) {
                    oldValues.put("sortOrder", opt.getSortOrder());
                }

                opt.setSortOrder(i);
                opt.setUpdatedAt(Instant.now());

                if (!oldValues.isEmpty()) {
                    recordHistory(opt, userId, ChangeType.UPDATED, oldValues);
                }
            }
        }


        optionRepository.saveAll(options);
    }


    // ------------------- TOGGLE ACTIVE -------------------
    @Override
    @Transactional
    public OptionEntity toggleActive(UUID id, boolean active, UUID userId) {
        OptionEntity option = optionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Option not found"));

        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("optionActive", option.isOptionActive());

        option.setOptionActive(active);
        option.setUpdatedBy(userId);
        option.setUpdatedAt(Instant.now());

        OptionEntity saved = optionRepository.save(option);

        recordHistory(saved, userId, ChangeType.UPDATED, oldValues);

        return saved;
    }
}


