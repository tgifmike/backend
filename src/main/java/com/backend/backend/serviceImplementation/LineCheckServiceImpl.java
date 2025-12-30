package com.backend.backend.serviceImplementation;

import com.backend.backend.dto.*;
import com.backend.backend.entity.*;
import com.backend.backend.repositories.*;
import com.backend.backend.service.LineCheckService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;


@Service
public class LineCheckServiceImpl implements LineCheckService {

    private final LineCheckRepository lineCheckRepository;
    private final LineCheckStationRepository lineCheckStationRepository;
    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final ItemRepository itemRepository;
    private final LineCheckItemRepository lineCheckItemRepository;

    public LineCheckServiceImpl(
            LineCheckRepository lineCheckRepository,
            UserRepository userRepository,
            StationRepository stationRepository,
            ItemRepository itemRepository,
            LineCheckStationRepository lineCheckStationRepository,
            LineCheckItemRepository lineCheckItemRepository
    ) {
        this.lineCheckRepository = lineCheckRepository;
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.itemRepository = itemRepository;
        this.lineCheckStationRepository = lineCheckStationRepository;
        this.lineCheckItemRepository = lineCheckItemRepository;
    }

    // ---------------------------------------------------------
    // CREATE NEW EMPTY LINE CHECK (fresh check)
    // ---------------------------------------------------------
   // @Override
    @Transactional
    public LineCheckDto createLineCheck(UUID userId, List<UUID> stationIds) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LineCheckEntity lineCheck = new LineCheckEntity();
        lineCheck.setUser(user);
        lineCheck.setCheckTime(Instant.now());
        lineCheck.setStations(new HashSet<>());

        LineCheckEntity savedLineCheck = lineCheckRepository.save(lineCheck);

        for (UUID stationId : stationIds) {
            StationEntity station = stationRepository.findById(stationId)
                    .orElseThrow(() -> new RuntimeException("Station not found"));

            LineCheckStationEntity lcs = new LineCheckStationEntity();
            lcs.setLineCheck(savedLineCheck);
            lcs.setStation(station);
            lcs.setLineCheckItems(new ArrayList<>());

            // Only create LineCheckItems once per check, from the template
            for (ItemEntity item : station.getItems()) {
                LineCheckItemEntity lci = new LineCheckItemEntity();
                lci.setLineCheckStation(lcs);
                lci.setStation(station);
                lci.setItem(item);
                lci.setItemChecked(false);
                lci.setChecked(false);
                lci.setItemNotes("");
                lci.setObservations("");
                lci.setTemperature(null);


                lcs.getLineCheckItems().add(lci);
            }

            lineCheckStationRepository.save(lcs);
            savedLineCheck.getStations().add(lcs);
        }

        return convertToDto(savedLineCheck);
    }

    // ---------------------------------------------------------
    // GET ALL LINE CHECKS (DTO LIST)
    // ---------------------------------------------------------
    @Override
    @Transactional
    public List<LineCheckDto> getAllLineChecksDto() {
        return lineCheckRepository.findAllByOrderByCheckTimeDesc()
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    // ---------------------------------------------------------
    // GET SINGLE LINE CHECK BY ID (DTO)
    // ---------------------------------------------------------
    @Override
    @Transactional
    public LineCheckEntity getLineCheckById(UUID id) {
        return lineCheckRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("LineCheck not found: " + id));
    }


    @Override
    @Transactional
    public LineCheckDto saveLineCheck(LineCheckDto dto) {
        if (dto.getId() == null) throw new IllegalArgumentException("LineCheck ID cannot be null");

        LineCheckEntity lineCheck = lineCheckRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("LineCheck not found: " + dto.getId()));

        for (LineCheckStationDto stationDto : dto.getStations()) {
            if (stationDto.getId() == null) continue;

            LineCheckStationEntity stationEntity = lineCheckStationRepository.findById(stationDto.getId())
                    .orElseThrow(() -> new RuntimeException("LineCheckStation not found: " + stationDto.getId()));

            if (stationDto.getItems() == null || stationDto.getItems().isEmpty()) continue;

            for (LineCheckItemDto itemDto : stationDto.getItems()) {
                if (itemDto.getId() == null) continue;

                LineCheckItemEntity itemEntity = lineCheckItemRepository.findById(itemDto.getId())
                        .orElseThrow(() -> new RuntimeException("LineCheckItem not found: " + itemDto.getId()));

                // ✅ Update entity fields
                itemEntity.setItemChecked(itemDto.isItemChecked());
                itemEntity.setChecked(itemDto.isItemChecked());

                if (itemDto.getTemperature() != null) {
                    itemEntity.setTemperature(itemDto.getTemperature());
                }
                if (itemDto.getObservations() != null) {
                    itemEntity.setObservations(itemDto.getObservations());
                }

                lineCheckItemRepository.save(itemEntity);
            }
        }

        if (lineCheck.getCompletedAt() == null) {
            lineCheck.setCompletedAt(Instant.now());
        }

        return convertToDto(lineCheckRepository.save(lineCheck));
    }


    @Transactional
    public List<LineCheckDto> getCompletedLineChecks() {
        return lineCheckRepository.findAllByCompletedAtIsNotNullOrderByCheckTimeDesc()
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    //get completed line checks by location
    @Transactional
    public List<LineCheckDto> getCompletedLineChecksByLocation(UUID locationId) {
        return lineCheckRepository
                .findDistinctByCompletedAtIsNotNullAndStations_Station_Location_IdOrderByCheckTimeDesc(locationId)
                .stream()
                .map(this::convertToDto)
                .toList();
    }





    // ============================================================
    // DTO CONVERSION HELPERS
    // ============================================================

    public LineCheckDto convertToDto(LineCheckEntity entity) {
        List<LineCheckStationDto> stationDtos = entity.getStations()
                .stream()
                .map(this::convertStationToDto)
                .toList();

        LineCheckDto dto = new LineCheckDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUser() != null ? entity.getUser().getId() : null);
        dto.setUsername(entity.getUser() != null ? entity.getUser().getUserName() : null);
        dto.setCheckTime(entity.getCheckTime());
        dto.setStations(stationDtos);
        dto.setCompletedAt(entity.getCompletedAt()); // <-- new

        return dto;
    }


    private LineCheckStationDto convertStationToDto(LineCheckStationEntity s) {
        List<LineCheckItemDto> itemDtos = s.getLineCheckItems()
                .stream()
                .map(this::convertItemToDto)
                .toList();

        LineCheckStationDto dto = new LineCheckStationDto();
        dto.setId(s.getId());
        dto.setStationName(s.getStation().getStationName());
        dto.setItems(itemDtos);

        return dto;
    }

private LineCheckItemDto convertItemToDto(LineCheckItemEntity e) {
    ItemEntity item = e.getItem();

    LineCheckItemDto dto = new LineCheckItemDto();
    dto.setId(e.getId());

    // Template fields
    dto.setItemName(item.getItemName());
    dto.setShelfLife(item.getShelfLife());
    dto.setPanSize(item.getPanSize());
    dto.setTool(item.getIsTool());
    dto.setToolName(item.getToolName());
    dto.setPortioned(item.getIsPortioned());
    dto.setPortionSize(item.getPortionSize());
    dto.setCheckMark(item.getIsCheckMark());  // ✅ template flag
    dto.setMinTemp(item.getMinTemp());
    dto.setMaxTemp(item.getMaxTemp());
    dto.setTemplateNotes(item.getTemplateNotes());
    dto.setSortOrder(item.getSortOrder());

    // User-entered fields (important!)
    dto.setItemChecked(e.isItemChecked());   // ✅ actual user check
    dto.setTempTaken(item.getIsTempTaken());    // ✅ can stay from template
    dto.setTemperature(e.getTemperature());  // ✅ user-entered
    dto.setObservations(e.getObservations()); // ✅ user-entered

    return dto;
}


    @Override
    @Transactional
    public LineCheckDto getLineCheckDtoById(UUID id) {
        LineCheckEntity entity = getLineCheckById(id); // reuse existing method
        return convertToDto(entity); // private converter is fine
    }
}


