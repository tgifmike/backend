package com.backend.backend.serviceImplementation;

import com.backend.backend.dto.LineCheckDto;
import com.backend.backend.dto.LineCheckItemDto;
import com.backend.backend.dto.LineCheckStationDto;
import com.backend.backend.entity.*;
import com.backend.backend.repositories.LineCheckRepository;
import com.backend.backend.service.LineCheckService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LineCheckServiceImpl implements LineCheckService {

    private final LineCheckRepository lineCheckRepository;

    public LineCheckServiceImpl(LineCheckRepository lineCheckRepository){
        this.lineCheckRepository = lineCheckRepository;
    }

    @Override
    @Transactional
    public LineCheckEntity createLineCheck(UserEntity user, List<StationEntity> stations) {
        LineCheckEntity lineCheck = new LineCheckEntity();
        lineCheck.setUser(user);
        lineCheck.setCheckTime(LocalDateTime.now());

        for (StationEntity station : stations) {
            LineCheckStationEntity lineCheckStation = new LineCheckStationEntity();
            lineCheckStation.setStation(station);
            lineCheckStation.setLineCheck(lineCheck);

            for (ItemEntity item : station.getItems()) {
                LineCheckItemEntity lineCheckItem = new LineCheckItemEntity();
                lineCheckItem.setItem(item);
                lineCheckItem.setLineCheckStation(lineCheckStation);
                lineCheckItem.setChecked(false); // default unchecked
                lineCheckStation.getItems().add(lineCheckItem);
            }

            lineCheck.getStations().add(lineCheckStation);
        }

        return lineCheckRepository.save(lineCheck);
    }

    @Override
    @Transactional
    public List<LineCheckDto> getAllLineChecksDto() {
        return lineCheckRepository.findAllByOrderByCheckTimeDesc()
                .stream()
                .map(lineCheck -> new LineCheckDto(
                        lineCheck.getId(),
                        lineCheck.getUser() != null ? lineCheck.getUser().getUserName() : "Unknown",
                        lineCheck.getCheckTime(),
                        lineCheck.getStations().stream()
                                .map(station -> new LineCheckStationDto(
                                        station.getId(),
                                        station.getStation().getStationName(),
                                        station.getItems().stream()
                                                .map(item -> {
                                                    ItemEntity it = item.getItem(); // full item
                                                    return new LineCheckItemDto(
                                                            item.getId(),
                                                            it.getItemName(),
                                                            it.getShelfLife(),
                                                            it.getPanSize(),
                                                            it.isTool(),
                                                            it.getToolName(),
                                                            it.isPortioned(),
                                                            it.getPortionSize(),
                                                            it.isTempTaken(),
                                                            it.isCheckMark(),
                                                            item.isChecked(), // current checkmark
                                                            item.getTemperature(),
                                                            it.getMinTemp(),
                                                            it.getMaxTemp(),// recorded temp
                                                            item.getNotes(), // notes added during line check
                                                            it.getItemNotes() // original item notes
                                                    );
                                                })
                                                .collect(Collectors.toList())
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    @Override
    public LineCheckEntity getLineCheckById(UUID id) {
        return lineCheckRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("LineCheck not found"));
    }

    @Override
    @Transactional
    public LineCheckEntity saveLineCheck(LineCheckDto lineCheckDto) {
        LineCheckEntity lineCheck = lineCheckRepository.findById(lineCheckDto.id())
                .orElseThrow(() -> new RuntimeException("LineCheck not found"));

        // Loop through stations
        for (LineCheckStationDto stationDto : lineCheckDto.stations()) {
            LineCheckStationEntity stationEntity = lineCheck.getStations().stream()
                    .filter(s -> s.getId().equals(stationDto.id()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("LineCheckStation not found: " + stationDto.id()));

            // Loop through items
            for (LineCheckItemDto itemDto : stationDto.items()) {
                LineCheckItemEntity itemEntity = stationEntity.getItems().stream()
                        .filter(i -> i.getId().equals(itemDto.id()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("LineCheckItem not found: " + itemDto.id()));

                // Update values from frontend
                itemEntity.setChecked(itemDto.checked());
                itemEntity.setTemperature(itemDto.temperature());
                itemEntity.setNotes(itemDto.notes());
            }
        }

        // Update timestamp
        lineCheck.setCheckTime(LocalDateTime.now());

        return lineCheckRepository.save(lineCheck);
    }

}


