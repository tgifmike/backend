package com.backend.backend.serviceImplementation;

import com.backend.backend.config.OptionType;
import com.backend.backend.dto.OptionCreateDto;
import com.backend.backend.entity.AccountEntity;
import com.backend.backend.entity.OptionEntity;
import com.backend.backend.repositories.AccountRepository;
import com.backend.backend.repositories.OptionRepository;
import com.backend.backend.service.OptionService;
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

    @Override
    public List<OptionEntity> getAllOptions(UUID accountId) {
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

        return optionRepository.save(option);
    }

    @Override
    @Transactional
    public OptionEntity updateOption(UUID optionId, OptionEntity option) {
        OptionEntity existing = optionRepository.findById(optionId)
                .orElseThrow(() -> new NoSuchElementException("Option not found"));

        existing.setOptionName(option.getOptionName());
        existing.setOptionActive(option.isOptionActive());
        existing.setOptionType(option.getOptionType());

        return optionRepository.save(existing);
    }

//    @Override
//    @Transactional
//    public void deleteOption(UUID optionId, UUID deletedByUser) {
//        OptionEntity option = optionRepository.findById(optionId)
//                .orElseThrow(() -> new NoSuchElementException("Option not found"));
//
//        // soft delete
//        option.setDeletedAt(Instant.now());
//        option.setDeletedBy(deletedByUser);
//        optionRepository.save(option);
//    }
@Transactional
@Override
public void deleteOption(UUID optionId, UUID deletedByUser) {
    log.warn("🔥 deleteOption CALLED for optionId={} by user={}", optionId, deletedByUser);

    OptionEntity option = optionRepository.findById(optionId)
            .orElseThrow(() -> new NoSuchElementException("Option not found"));

    Instant now = Instant.now();
    option.setDeletedAt(now);
    option.setDeletedBy(deletedByUser);

    // Force Hibernate to treat the entity as dirty
    option.setUpdatedAt(now);

    // log
    log.info("Deleting option {}: deletedAt={}, deletedBy={}, updatedAt={}",
            optionId, option.getDeletedAt(), option.getDeletedBy(), option.getUpdatedAt());

    optionRepository.saveAndFlush(option);
    log.info("Saved and flushed delete for option {}", optionId);
}




    @Override
    @Transactional
    public void reorderOptions(UUID accountId, OptionType optionType, List<UUID> orderedIds) {
        List<OptionEntity> options = (optionType != null)
                ? optionRepository.findByAccountIdAndOptionTypeOrderBySortOrderAsc(accountId, optionType)
                : optionRepository.findByAccountIdOrderBySortOrderAsc(accountId);

        Map<UUID, OptionEntity> optionMap = options.stream()
                .collect(Collectors.toMap(OptionEntity::getId, o -> o));

        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            OptionEntity opt = optionMap.get(id);
            if (opt != null) opt.setSortOrder(i);
        }

        optionRepository.saveAll(options);
    }

    @Override
    public List<OptionEntity> getOptionsByAccount(UUID accountId) {
        return optionRepository.findByAccountIdOrderBySortOrderAsc(accountId);
    }

    @Override
    @Transactional
    public OptionEntity toggleActive(UUID id, boolean active, UUID userId) {
        OptionEntity option = optionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Option not found"));

        option.setOptionActive(active);
        option.setUpdatedBy(userId);

        return optionRepository.save(option);
    }
}


