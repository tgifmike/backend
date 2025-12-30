package com.backend.backend.controller;

import com.backend.backend.entity.OptionHistoryEntity;
import com.backend.backend.enums.OptionType;
import com.backend.backend.config.UserContext;
import com.backend.backend.dto.OptionAuditDto;
import com.backend.backend.dto.OptionCreateDto;
import com.backend.backend.entity.OptionEntity;
import com.backend.backend.repositories.OptionHistoryRepository;
import com.backend.backend.repositories.OptionRepository;
import com.backend.backend.service.OptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/options")
@RequiredArgsConstructor
public class OptionController {

    private final OptionService optionService;
    private final OptionRepository optionRepository;
    private final OptionHistoryRepository optionHistoryRepository;

    @GetMapping
    public ResponseEntity<List<OptionEntity>> getOptions(
            @RequestParam UUID accountId,
            @RequestParam(required = false) OptionType optionType
    ) {
        List<OptionEntity> options = (optionType != null)
                ? optionService.getOptionsByType(accountId, optionType)
                : optionService.getAllOptions(accountId);

        return ResponseEntity.ok(options);
    }

    @PostMapping
    public ResponseEntity<OptionEntity> createOption(
            @RequestBody @Valid OptionCreateDto dto,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(optionService.createOption(dto, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OptionEntity> updateOption(
            @PathVariable UUID id,
            @RequestBody OptionEntity option,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(optionService.updateOption(id, option, userId));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOption(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId
    ) {

        //log.warn("🔥 CONTROLLER deleteOption HIT for id={} user={}", id, userId);

        UserContext.setCurrentUser(userId);

        optionService.deleteOption(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderOptions(
            @RequestParam UUID accountId,
            @RequestParam(required = false) OptionType optionType,
            @RequestBody List<UUID> orderedOptionIds,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        optionService.reorderOptions(accountId, optionType, orderedOptionIds, userId);
        return ResponseEntity.ok().build();
    }



    @PutMapping("/{id}/active")
    public ResponseEntity<OptionEntity> toggleActive(
            @PathVariable UUID id,
            @RequestParam boolean active,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(optionService.toggleActive(id, active, userId));
    }


    @GetMapping("/history")
    public List<OptionHistoryEntity> getHistory(@RequestParam UUID accountId) {
        return optionHistoryRepository
                .findByAccountIdOrderByChangeAtDesc(accountId);
    }

}

