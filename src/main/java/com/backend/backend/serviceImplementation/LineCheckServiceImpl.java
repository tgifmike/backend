package com.backend.backend.serviceImplementation;

import com.backend.backend.config.StartOfWeek;
import com.backend.backend.dto.*;
import com.backend.backend.entity.*;
import com.backend.backend.repositories.*;
import com.backend.backend.service.LineCheckService;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class LineCheckServiceImpl implements LineCheckService {

    private final LineCheckRepository lineCheckRepository;
    private final LineCheckStationRepository lineCheckStationRepository;
    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    //private final ItemRepository itemRepository;
    private final LineCheckItemRepository lineCheckItemRepository;
    private final LocationRepository locationRepository;

    public LineCheckServiceImpl(
            LineCheckRepository lineCheckRepository,
            UserRepository userRepository,
            StationRepository stationRepository,
            //  ItemRepository itemRepository,
            LineCheckStationRepository lineCheckStationRepository,
            LineCheckItemRepository lineCheckItemRepository,
            LocationRepository locationRepository
    ) {
        this.lineCheckRepository = lineCheckRepository;
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        // this.itemRepository = itemRepository;
        this.lineCheckStationRepository = lineCheckStationRepository;
        this.lineCheckItemRepository = lineCheckItemRepository;
        this.locationRepository = locationRepository;
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
                itemEntity.setMissing(itemDto.getMissing());

                if (itemDto.getMissing()) {
                    itemEntity.setTemperature(null);
                } else {
                    itemEntity.setTemperature(itemDto.getTemperature());
                }

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

        if (entity.getCompletedAt() != null) {

            long seconds =
                    entity.getCompletedAt().getEpochSecond()
                            - entity.getCheckTime().getEpochSecond();

            dto.setDurationSeconds(seconds);
        }

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
        dto.setMissing(e.isMissing());

        return dto;
    }


    @Override
    @Transactional
    public LineCheckDto getLineCheckDtoById(UUID id) {
        LineCheckEntity entity = getLineCheckById(id); // reuse existing method
        return convertToDto(entity); // private converter is fine
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardMetricsDto getDashboardMetrics(UUID locationId) {

        DashboardMetricsDto dto = new DashboardMetricsDto();

        // -------------------------------
        // Load location configuration
        // -------------------------------
        LocationEntity location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found"));

        String tz = location.getLocationTimeZone();
        ZoneId zone;
        try {
            zone = tz != null ? ZoneId.of(tz) : ZoneId.systemDefault();
        } catch (DateTimeException e) {
            // fallback to a default or map manually
            switch (tz) {
                case "Eastern Time (GMT-5)" -> zone = ZoneId.of("America/New_York");
                case "Central Time (GMT-6)" -> zone = ZoneId.of("America/Chicago");
                default -> zone = ZoneId.systemDefault();
            }
        }

        LocalDate today = LocalDate.now(zone);
        DayOfWeek startDay = location.getStartOfWeek() == StartOfWeek.SUNDAY
                ? DayOfWeek.SUNDAY
                : DayOfWeek.MONDAY;

        // -------------------------------
        // Date boundaries
        // -------------------------------
        Instant startOfDay = today.atStartOfDay(zone).toInstant();
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);

        Instant startOfYesterday = startOfDay.minus(1, ChronoUnit.DAYS);
        Instant startOfWeek = today.with(TemporalAdjusters.previousOrSame(startDay))
                .atStartOfDay(zone).toInstant();
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();
        Instant now = Instant.now();
        Instant last30Days = Instant.now().minus(30, ChronoUnit.DAYS);

        // -------------------------------
        // Line check totals
        // -------------------------------
        dto.setTotalChecksToday(lineCheckRepository.countChecksToday(locationId, startOfDay, endOfDay));
        dto.setTotalChecksYesterday(lineCheckRepository.countChecksToday(locationId, startOfYesterday, startOfDay));
        dto.setTotalChecksWeekToDate(lineCheckRepository.countChecksWeekToDate(locationId, startOfWeek));
        dto.setTotalChecksMonthToDate(lineCheckRepository.countChecksMonthToDate(locationId, startOfMonth));

        // -------------------------------
        // Employee productivity metrics
        // -------------------------------
        dto.setEmployeeChecksToday(lineCheckRepository.countChecksPerEmployee(locationId, startOfDay, endOfDay));
        dto.setEmployeeChecksWeek(lineCheckRepository.countChecksPerEmployee(locationId, startOfWeek, now));
        dto.setEmployeeChecksMonth(lineCheckRepository.countChecksPerEmployee(locationId, startOfMonth, now));

        // -------------------------------
        // Employee performance metrics (new)
        // -------------------------------
        List<LineCheckEntity> checks = lineCheckRepository.employeePerformance(locationId, startOfDay, endOfDay);

        List<EmployeePerformanceDto> performanceList = checks.stream()
                .collect(Collectors.groupingBy(LineCheckEntity::getUser)) // group by employee
                .entrySet().stream()
                .map(e -> {
                    var user = e.getKey();
                    var userChecks = e.getValue();
                    long count = userChecks.size();
                    double avgSeconds = userChecks.stream()
                            .filter(lc -> lc.getCompletedAt() != null)
                            .mapToLong(lc -> Duration.between(lc.getCheckTime(), lc.getCompletedAt()).getSeconds())
                            .average().orElse(0);
                    return new EmployeePerformanceDto(user.getId(), user.getUserName(), count, avgSeconds);
                })
                .toList();

        dto.setEmployeePerformanceToday(performanceList);

        // -------------------------------
        // Issue summary totals (today)
        // -------------------------------
        dto.setMissingItemsToday(
                lineCheckItemRepository.countMissingItemsToday(locationId, startOfDay, endOfDay)
        );
        dto.setMissingItemNamesToday(
                lineCheckItemRepository.findMissingItemNamesToday(locationId, startOfDay, endOfDay)
        );
        dto.setOutOfTempItemsToday(
                lineCheckItemRepository.countOutOfTempItemsToday(locationId, startOfDay, endOfDay)
        );
        dto.setOutOfTempItemNamesToday(
                lineCheckItemRepository.findOutOfTempItemNamesToday(locationId, startOfDay, endOfDay)
        );
        dto.setIncorrectPrepItemsToday(
                lineCheckItemRepository.countIncorrectPrepItemsToday(locationId, startOfDay, endOfDay)
        );
        dto.setIncorrectPrepItemNamesToday(
                lineCheckItemRepository.findIncorrectPrepItemNamesToday(locationId, startOfDay, endOfDay)
        );

        // -------------------------------
        // Average completion duration (today)
        // -------------------------------
        Double avgSeconds = lineCheckRepository.avgCompletionSecondsToday(locationId, startOfDay, endOfDay);
        dto.setDurationSeconds(avgSeconds != null ? avgSeconds.longValue() : 0L);

        // -------------------------------
        // Detailed issue breakdown (today)
        // -------------------------------
        List<LineCheckItemIssuesDto> issueDtos = new ArrayList<>();
        List<LineCheckEntity> checksToday = lineCheckRepository.findByLocationAndCheckTimeBetween(
                locationId, startOfDay, endOfDay
        );

        // -------------------------------
        // total missed items
        // -------------------------------

        dto.setMostMissingItemsDay(
                extractTopDay(
                        lineCheckItemRepository.missingItemsByWeekday(locationId, last30Days)
                )
        );

        dto.setMostOutOfTempDay(
                extractTopDay(
                        lineCheckItemRepository.outOfTempByWeekday(locationId, last30Days)
                )
        );

        dto.setMostIncorrectPrepDay(
                extractTopDay(
                        lineCheckItemRepository.incorrectPrepByWeekday(locationId, last30Days)
                )
        );

        dto.setWeakestLineCheckDay(
                extractTopDay(
                        lineCheckRepository.weakestCheckDays(locationId, last30Days)
                )
        );


        for (LineCheckEntity lc : checksToday) {
            LineCheckItemIssuesDto issuesDto = new LineCheckItemIssuesDto();
            issuesDto.setLineCheckId(lc.getId());
            issuesDto.setCheckTime(lc.getCheckTime());
            if (lc.getUser() != null) {
                issuesDto.setEmployeeName(lc.getUser().getUserName());
            }

            List<String> missing = lineCheckItemRepository.findMissingItemNamesByLineCheck(lc.getId());
            List<String> outOfTemp = lineCheckItemRepository.findOutOfTempItemNamesByLineCheck(lc.getId());
            List<String> incorrectPrep = lineCheckItemRepository.findIncorrectPrepItemNamesByLineCheck(lc.getId());

            issuesDto.setMissingItems(missing);
            issuesDto.setMissingCount(missing.size());
            issuesDto.setOutOfTempItems(outOfTemp);
            issuesDto.setOutOfTempCount(outOfTemp.size());
            issuesDto.setIncorrectPrepItems(incorrectPrep);
            issuesDto.setIncorrectPrepCount(incorrectPrep.size());

            issueDtos.add(issuesDto);
        }

        dto.setLineChecks(issueDtos);

        // -------------------------------
        // Return dashboard payload
        // -------------------------------
        return dto;
    }

    private String extractTopDay(List<Object[]> results) {
        if (results == null || results.isEmpty()) return "N/A";

        return results.get(0)[0].toString().trim();
    }


}