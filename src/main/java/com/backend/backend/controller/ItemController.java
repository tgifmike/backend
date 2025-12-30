package com.backend.backend.controller;

import com.backend.backend.config.UserContext;
import com.backend.backend.dto.ItemCreateDto;
import com.backend.backend.dto.ItemUpdateDto;
import com.backend.backend.dto.OptionCreateDto;
import com.backend.backend.entity.ItemEntity;
import com.backend.backend.entity.ItemHistoryEntity;
import com.backend.backend.entity.OptionEntity;
import com.backend.backend.entity.OptionHistoryEntity;
import com.backend.backend.enums.OptionType;
import com.backend.backend.repositories.ItemHistoryRepository;
import com.backend.backend.repositories.ItemRepository;
import com.backend.backend.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final ItemRepository itemRepository;
    private final ItemHistoryRepository itemHistoryRepository;


    /**
     * Create a new item tied to a specific station.
     */
    @PostMapping("{stationId}/createItem")
    public ResponseEntity<ItemEntity> createItem(
            @RequestBody @Valid ItemCreateDto dto,
            @RequestHeader("X-User-Id") UUID userId
            ) {
        return ResponseEntity.ok(itemService.createItem(dto, userId));
    }


    /**
     * Get all items for a given station.
     */
    @GetMapping("{stationId}/getAllItems")
    public ResponseEntity<List<ItemEntity>> getItemsByStation(@PathVariable UUID stationId) {
        List<ItemEntity> items = itemService.getItemsByStation(stationId);
        return ResponseEntity.ok(items);
    }

    /**
     * Get a single item by ID.
     */
    @GetMapping("{stationId}/getItem/{itemId}")
    public ResponseEntity<ItemEntity> getItemById(@PathVariable UUID itemId) {
        ItemEntity item = itemService.getItemById(itemId);
        return ResponseEntity.ok(item);
    }

    /**
     * Update an item.
     */
    @PutMapping("{stationId}/updateItem/{itemId}")
    public ResponseEntity<ItemEntity> updateItem(
            @PathVariable UUID stationId,
            @PathVariable("itemId") UUID itemId,
            @RequestBody ItemUpdateDto dto,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(itemService.updateItem(stationId, itemId, dto, userId));
    }


    /**
     * Toggle active/inactive status (soft delete).
     */
    @PatchMapping("{stationId}/{itemId}/active")
    public ResponseEntity<ItemEntity> toggleItemActive(
            @PathVariable UUID stationId,
            @PathVariable UUID itemId,
            @RequestParam boolean active,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(itemService.toggleActive(stationId, itemId, active, userId));
    }

    /**
     * delete an item
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable("itemId") UUID itemId,
            @RequestHeader("X-User-Id") UUID userId)
    {
        UserContext.setCurrentUser(userId);
        itemService.deleteItem(itemId, userId);
        return ResponseEntity.noContent().build();
    }


    /**
     * reorder an item
     */
    @PutMapping("/{stationId}/reorder")
    public ResponseEntity<Void> reorderItems(
            @PathVariable UUID stationId,
            @RequestBody List<UUID> orderedOptionIds,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        itemService.reorderItems(stationId, orderedOptionIds, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history")
    public List<ItemHistoryEntity> getHistory(@RequestParam UUID stationId) {
        return itemHistoryRepository
                .findByStationIdOrderByChangeAtDesc(stationId);
    }


}

