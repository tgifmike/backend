package com.backend.backend.controller;

import com.backend.backend.config.OptionType;
import com.backend.backend.dto.OptionCreateDto;
import com.backend.backend.entity.OptionEntity;
import com.backend.backend.service.OptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/options")
@RequiredArgsConstructor
public class OptionController {

    private final OptionService optionService;

    /**
     * Get all options for an account, optionally filtered by optionType
     *
     * Frontend should include current user UUID in the header:
     * X-User-Id: <uuid>
     */
    @GetMapping
    public ResponseEntity<List<OptionEntity>> getOptions(
            @RequestParam UUID accountId,
            @RequestParam(required = false) OptionType optionType
    ) {
        List<OptionEntity> options;
        if (optionType != null) {
            options = optionService.getOptionsByType(accountId, optionType);
        } else {
            options = optionService.getAllOptions(accountId);
        }
        return ResponseEntity.ok(options);
    }

    /**
     * Create a new option
     *
     * Frontend must send X-User-Id header for auditing:
     * X-User-Id: <uuid>
     */
    @PostMapping
    public ResponseEntity<OptionEntity> createOption(
            @RequestBody @Valid OptionCreateDto dto,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        OptionEntity created = optionService.createOption(dto, userId);
        return ResponseEntity.ok(created);
    }

    /**
     * Update an existing option
     */
    @PutMapping("/{id}")
    public ResponseEntity<OptionEntity> updateOption(
            @PathVariable UUID id,
            @RequestBody OptionEntity option
    ) {
        OptionEntity updated = optionService.updateOption(id, option);
        return ResponseEntity.ok(updated);
    }

    /**
     * Soft delete an option
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOption(@PathVariable UUID id) {
        optionService.deleteOption(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reorder options after drag & drop
     */

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderOptions(
            @RequestParam UUID accountId,
            @RequestParam(required = false) OptionType optionType,
            @RequestBody List<UUID> orderedOptionIds
    ) {
        optionService.reorderOptions(accountId, optionType, orderedOptionIds);
        return ResponseEntity.ok().build();
    }




    /**
     * Toggle active status for an option
     */
    @PutMapping("/{id}/active")
    public ResponseEntity<OptionEntity> toggleActive(
            @PathVariable UUID id,
            @RequestParam boolean active,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        OptionEntity updated = optionService.toggleActive(id, active, userId);
        return ResponseEntity.ok(updated);
    }

}
