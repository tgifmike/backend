package com.backend.backend.controller;

import com.backend.backend.dto.TemperatureCategoryCreateDto;
import com.backend.backend.dto.TemperatureCategoryDto;
import com.backend.backend.dto.TemperatureCategoryUpdateDto;
import com.backend.backend.service.TemperatureCategoryService;
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
@RequestMapping("/temperature-categories")
@RequiredArgsConstructor
public class TemperatureCategoryController {

    private final TemperatureCategoryService temperatureCategoryService;

    @GetMapping
    public List<TemperatureCategoryDto> getByLocation(
            @RequestParam UUID locationId
    ) {
        return temperatureCategoryService.getByLocation(locationId);
    }

    @PostMapping
    public ResponseEntity<TemperatureCategoryDto> create(
            @Valid @RequestBody TemperatureCategoryCreateDto dto
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(temperatureCategoryService.create(dto));
    }

    @PutMapping("/{categoryId}")
    public TemperatureCategoryDto update(
            @PathVariable UUID categoryId,
            @Valid @RequestBody TemperatureCategoryUpdateDto dto
    ) {
        return temperatureCategoryService.update(categoryId, dto);
    }

    @PatchMapping("/{categoryId}/active")
    public TemperatureCategoryDto setActive(
            @PathVariable UUID categoryId,
            @RequestParam boolean active
    ) {
        return temperatureCategoryService.setActive(categoryId, active);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(@PathVariable UUID categoryId) {
        temperatureCategoryService.delete(categoryId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/location/{locationId}/defaults")
    public List<TemperatureCategoryDto> restoreDefaults(
            @PathVariable UUID locationId
    ) {
        return temperatureCategoryService.restoreDefaults(locationId);
    }
}
