package com.backend.backend.serviceImplementation;

import com.backend.backend.config.OptionType;
import com.backend.backend.dto.OptionCreateDto;
import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.OptionEntity;
import com.backend.backend.repositories.AccountRepository;
import com.backend.backend.repositories.OptionRepository;
import com.backend.backend.service.OptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OptionServiceImpl implements OptionService {

    private final OptionRepository optionRepository;
    private final AccountRepository accountRepository;

    @Override
    public List<OptionEntity> getAllOptions(UUID accountId) {
        // Fetch all active options sorted by sortOrder
        return optionRepository.findByAccountIdOrderBySortOrderAsc(accountId);
    }

    @Override
    public List<OptionEntity> getOptionsByType(UUID accountId, OptionType optionType) {
        return optionRepository.findByAccountIdAndOptionTypeOrderBySortOrderAsc(accountId, optionType);
    }

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
        return saved; // will have ID, createdAt, etc.
    }



    @Override
    public OptionEntity updateOption(UUID optionId, OptionEntity option) {
        OptionEntity existing = optionRepository.findById(optionId)
                .orElseThrow(() -> new NoSuchElementException("Option not found"));

        existing.setOptionName(option.getOptionName());
        existing.setOptionActive(option.isOptionActive());
        existing.setOptionType(option.getOptionType());
        // DO NOT update account here
        // existing.setAccount(option.getAccount());

        return optionRepository.save(existing);
    }


    @Override
    public void deleteOption(UUID optionId) {
        OptionEntity existing = optionRepository.findById(optionId)
                .orElseThrow(() -> new NoSuchElementException("Option not found"));
        optionRepository.delete(existing); // soft delete due to @SQLDelete
    }

    @Override
    public void reorderOptions(UUID accountId, OptionType optionType, List<UUID> orderedIds) {
        List<OptionEntity> options;
        if (optionType != null) {
            options = optionRepository.findByAccountIdAndOptionTypeOrderBySortOrder(accountId, optionType);
        } else {
            options = optionRepository.findByAccountIdOrderBySortOrder(accountId);
        }

        Map<UUID, OptionEntity> optionMap = options.stream()
                .collect(Collectors.toMap(OptionEntity::getId, o -> o));

        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            OptionEntity opt = optionMap.get(id);
            if (opt != null) {
                opt.setSortOrder(i);
            }
        }

        optionRepository.saveAll(options);
    }

    @Override
    public List<OptionEntity> getOptionsByAccount(UUID accountId) {
        return optionRepository
                .findByAccountIdOrderBySortOrderAsc(accountId);
    }

    @Override
    @Transactional
    public OptionEntity toggleActive(UUID id, boolean active, UUID userId) {
        OptionEntity option = optionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Option not found"
                ));

        option.setOptionActive(active);
        option.setUpdatedBy(userId);

        return optionRepository.save(option);
    }


}

