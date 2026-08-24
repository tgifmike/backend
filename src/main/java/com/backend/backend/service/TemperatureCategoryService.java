package com.backend.backend.service;

import com.backend.backend.dto.TemperatureCategoryCreateDto;
import com.backend.backend.dto.TemperatureCategoryDto;
import com.backend.backend.dto.TemperatureCategoryUpdateDto;

import java.util.List;
import java.util.UUID;

public interface TemperatureCategoryService {

    List<TemperatureCategoryDto> getByLocation(UUID locationId);

    TemperatureCategoryDto create(TemperatureCategoryCreateDto dto);

    TemperatureCategoryDto update(UUID categoryId, TemperatureCategoryUpdateDto dto);

    TemperatureCategoryDto setActive(UUID categoryId, boolean active);

    void delete(UUID categoryId);

    List<TemperatureCategoryDto> restoreDefaults(UUID locationId);
}
