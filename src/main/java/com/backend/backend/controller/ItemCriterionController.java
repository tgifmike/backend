package com.backend.backend.controller;

import com.backend.backend.dto.ItemCriterionDto;
import com.backend.backend.dto.ItemCriterionRequestDto;
import com.backend.backend.service.ItemCriterionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/items/{itemId}/criteria")
@RequiredArgsConstructor
public class ItemCriterionController {

    private final ItemCriterionService itemCriterionService;

    @GetMapping
    public List<ItemCriterionDto> getByItem(@PathVariable UUID itemId) {
        return itemCriterionService.getByItem(itemId);
    }

    @PostMapping
    public ResponseEntity<ItemCriterionDto> create(
            @PathVariable UUID itemId,
            @Valid @RequestBody ItemCriterionRequestDto dto
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(itemCriterionService.create(itemId, dto));
    }

    @PutMapping("/{criterionId}")
    public ItemCriterionDto update(
            @PathVariable UUID itemId,
            @PathVariable UUID criterionId,
            @Valid @RequestBody ItemCriterionRequestDto dto
    ) {
        return itemCriterionService.update(itemId, criterionId, dto);
    }

    @PatchMapping("/{criterionId}/active")
    public ItemCriterionDto setActive(
            @PathVariable UUID itemId,
            @PathVariable UUID criterionId,
            @RequestParam boolean active
    ) {
        return itemCriterionService.setActive(itemId, criterionId, active);
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorder(
            @PathVariable UUID itemId,
            @RequestBody List<UUID> orderedCriterionIds
    ) {
        itemCriterionService.reorder(itemId, orderedCriterionIds);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{criterionId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID itemId,
            @PathVariable UUID criterionId
    ) {
        itemCriterionService.delete(itemId, criterionId);
        return ResponseEntity.noContent().build();
    }
}
