package com.backend.backend.serviceImplementation;

import com.backend.backend.dto.*;
import com.backend.backend.entity.*;
import com.backend.backend.repositories.*;
import com.backend.backend.service.LineCheckService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

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
        lineCheck.setCheckTime(LocalDateTime.now());
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


    // ---------------------------------------------------------
    // SAVE/UPDATE LINE CHECK (from mobile app)
    // ---------------------------------------------------------
//    @Override
//    @Transactional
//    public LineCheckEntity saveLineCheck(LineCheckDto dto) {
//
//        if (dto.getId() == null) {
//            throw new IllegalArgumentException("LineCheck ID cannot be null when saving.");
//        }
//
//        LineCheckEntity lineCheck = lineCheckRepository.findById(dto.getId())
//                .orElseThrow(() -> new RuntimeException("LineCheck not found: " + dto.getId()));
//
//        // Record time completed
//        lineCheck.setCheckTime(LocalDateTime.now());
//
//        System.out.println(">>> dto.getId(): " + dto.getId());
//
//        for (LineCheckStationDto stationDto : dto.getStations()) {
//
//            System.out.println(">>> stationDto.getId(): " + stationDto.getId());
//
//            LineCheckStationEntity stationEntity = lineCheckStationRepository.findById(stationDto.getId())
//                    .orElseThrow(() -> new RuntimeException("LineCheckStation not found: " + stationDto.getId()));
//
//            for (LineCheckItemDto itemDto : stationDto.getItems()) {
//                System.out.println(">>> itemDto.getId(): " + itemDto.getId());
//                LineCheckItemEntity itemEntity = lineCheckItemRepository.findById(itemDto.getId())
//                        .orElseThrow(() -> new RuntimeException("LineCheckItem not found: " + itemDto.getId()));
//
//                itemEntity.setItemChecked(itemDto.isItemChecked());
//                itemEntity.setChecked(itemDto.isItemChecked());
//                itemEntity.setTemperature(itemDto.getTemperature());
//                itemEntity.setNotes(itemDto.getNotes());
//
//                lineCheckItemRepository.save(itemEntity);
//            }
//        }
//
//        return lineCheckRepository.save(lineCheck);
//    }

    @Override
    @Transactional
    public LineCheckDto saveLineCheck(LineCheckDto dto) {
        if (dto.getId() == null) {
            throw new IllegalArgumentException("LineCheck ID cannot be null");
        }

        LineCheckEntity lineCheck = lineCheckRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("LineCheck not found: " + dto.getId()));

        // Update each existing LineCheckStation
        for (LineCheckStationDto stationDto : dto.getStations()) {
            if (stationDto.getId() == null) continue; // Skip any new stations (shouldn’t happen)

            LineCheckStationEntity stationEntity = lineCheckStationRepository.findById(stationDto.getId())
                    .orElseThrow(() -> new RuntimeException("LineCheckStation not found: " + stationDto.getId()));

            // Update only existing LineCheckItems
            for (LineCheckItemDto itemDto : stationDto.getItems()) {
                if (itemDto.getId() == null) continue; // Skip template items

                LineCheckItemEntity itemEntity = lineCheckItemRepository.findById(itemDto.getId())
                        .orElseThrow(() -> new RuntimeException("LineCheckItem not found: " + itemDto.getId()));

                itemEntity.setItemChecked(itemDto.isItemChecked());
                itemEntity.setChecked(itemDto.isItemChecked());
                itemEntity.setTemperature(itemDto.getTemperature());
                itemEntity.setItemNotes(itemDto.getItemNotes());
                itemEntity.setObservations(itemDto.getObservations());

                lineCheckItemRepository.save(itemEntity);
            }
        }

        // Save the updated LineCheck
        return convertToDto(lineCheckRepository.save(lineCheck));
    }


    // ---------------------------------------------------------
    // UPDATE LINE CHECK (from mobile app)
    // ---------------------------------------------------------

//    @Override
//    @Transactional
//    public void updateLineCheck(LineCheckSaveDto dto) {
//        LineCheckEntity lineCheck = lineCheckRepository.findById(dto.id())
//                .orElseThrow(() -> new RuntimeException("LineCheck not found: " + dto.id()));
//
//        for (LineCheckStationUpdateDto stationDto : dto.stations()) {
//            LineCheckStationEntity station = lineCheck.getStations()
//                    .stream()
//                    .filter(s -> s.getId().equals(stationDto.stationId()))
//                    .findFirst()
//                    .orElseThrow(() -> new RuntimeException("Station not found: " + stationDto.stationId()));
//
//            for (LineCheckItemUpdateDto itemDto : stationDto.items()) {
//                LineCheckItemEntity item = station.getLineCheckItems()
//                        .stream()
//                        .filter(i -> i.getId().equals(itemDto.id()))
//                        .findFirst()
//                        .orElseThrow(() -> new RuntimeException("Item not found: " + itemDto.id()));
//
//                item.setChecked(itemDto.checked());
//                item.setItemChecked(itemDto.checked());
//                item.setTemperature(itemDto.temperature());
//                item.setNotes(itemDto.notes());
//
//                lineCheckItemRepository.save(item);
//            }
//        }
//
//        lineCheckRepository.save(lineCheck);
//    }


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

        // From ItemEntity
        dto.setItemName(item.getItemName());
        dto.setShelfLife(item.getShelfLife());
        dto.setPanSize(item.getPanSize());
        dto.setTool(item.isTool());
        dto.setToolName(item.getToolName());
        dto.setPortioned(item.isPortioned());
        dto.setPortionSize(item.getPortionSize());
        dto.setTempTaken(item.isTempTaken());
        dto.setCheckMark(item.isCheckMark());
        dto.setMinTemp(item.getMinTemp());
        dto.setMaxTemp(item.getMaxTemp());
        dto.setItemNotes(item.getItemNotes());
        dto.setSortOrder(item.getSortOrder());

        // From LineCheckItemEntity
        dto.setItemChecked(e.isItemChecked());
        dto.setTemperature(e.getTemperature());
        dto.setItemNotes(e.getItemNotes());
        dto.setObservations(e.getObservations());

        return dto;
    }

    @Override
    @Transactional
    public LineCheckDto getLineCheckDtoById(UUID id) {
        LineCheckEntity entity = getLineCheckById(id); // reuse existing method
        return convertToDto(entity); // private converter is fine
    }
}


