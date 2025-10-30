package com.backend.backend.controller;

import com.backend.backend.entity.ItemEntity;
import com.backend.backend.repositories.ItemRepository;
import com.backend.backend.service.ItemService;
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
public class ItemController {

    private final ItemService itemService;
    private final ItemRepository itemRepository;

    public ItemController (ItemService itemService, ItemRepository itemRepository){
        this.itemService = itemService;
        this.itemRepository = itemRepository;
    }

    /**
     * Create a new item tied to a specific station.
     */
    @PostMapping("{stationId}/createItem")
    public ResponseEntity<?> createItem(@PathVariable UUID stationId, @RequestBody ItemEntity itemRequest) {
        try {
            ItemEntity createdItem = itemService.createItem(stationId, itemRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
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
    @PatchMapping("{stationId}/updateItem/{itemId}")
    public ResponseEntity<?> updateItem(@PathVariable UUID itemId, @RequestBody ItemEntity updatedItem) {
        try {
            ItemEntity item = itemService.updateItem(itemId, updatedItem);
            return ResponseEntity.ok(item);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /**
     * Toggle active/inactive status (soft delete).
     */
    @PatchMapping("{stationId}/{itemId}/active")
    public ResponseEntity<ItemEntity> toggleItemActive(
            @PathVariable UUID itemId,
            @RequestParam boolean active
    ) {
        ItemEntity updated = itemService.toggleActive(itemId, active);
        return ResponseEntity.ok(updated);
    }

    /**
     * Permanently delete an item (optional).
     */
    @DeleteMapping("/{stationId}/deleteItem/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID itemId) {
        itemService.deleteItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{stationId}/items/reorder")
    public ResponseEntity<Void> reorderItems(
            @PathVariable UUID stationId,
            @RequestBody List<UUID> itemIdsInOrder
    ) {
        List<ItemEntity> items = itemRepository.findAllById(itemIdsInOrder);

        // Create a lookup map for items by ID
        Map<UUID, ItemEntity> itemMap = items.stream()
                .collect(Collectors.toMap(ItemEntity::getId, Function.identity()));

        // Assign new order directly
        for (int i = 0; i < itemIdsInOrder.size(); i++) {
            UUID itemId = itemIdsInOrder.get(i);
            ItemEntity item = itemMap.get(itemId);
            if (item != null) {
                item.setSortOrder(i + 1);
            }
        }

        itemRepository.saveAll(items);
        return ResponseEntity.ok().build();
    }

}

