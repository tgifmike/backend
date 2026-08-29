package com.backend.backend.serviceImplementation;

import com.backend.backend.config.UserContext;
import com.backend.backend.enums.StartOfWeek;
import com.backend.backend.enums.ItemType;
import com.backend.backend.enums.ResponseType;
import com.backend.backend.dto.*;
import com.backend.backend.entity.*;
import com.backend.backend.repositories.*;
import com.backend.backend.service.LineCheckService;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
                lci.setMinTemp(item.getMinTemp());
                lci.setMaxTemp(item.getMaxTemp());

                List<LineCheckCriterionResponseEntity> criterionResponses = item.getCriteria()
                        .stream()
                        .filter(criterion -> Boolean.TRUE.equals(criterion.getActive()))
                        .map(criterion -> LineCheckCriterionResponseEntity.builder()
                                .lineCheckItem(lci)
                                .itemCriterionId(criterion.getId())
                                .label(criterion.getLabel())
                                .responseType(criterion.getResponseType())
                                .required(Boolean.TRUE.equals(criterion.getRequired()))
                                .requireNotesOnFailure(
                                        Boolean.TRUE.equals(criterion.getRequireNotesOnFailure())
                                )
                                .minValue(criterion.getMinValue())
                                .maxValue(criterion.getMaxValue())
                                .unit(criterion.getUnit())
                                .sortOrder(criterion.getSortOrder())
                                .build())
                        .toList();
                lci.setCriterionResponses(new ArrayList<>(criterionResponses));


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

            if (stationEntity.getLineCheck() == null
                    || !lineCheck.getId().equals(stationEntity.getLineCheck().getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Line check station does not belong to line check " + lineCheck.getId()
                );
            }

            if (stationDto.getItems() == null || stationDto.getItems().isEmpty()) continue;

            for (LineCheckItemDto itemDto : stationDto.getItems()) {
                if (itemDto.getId() == null) continue;

                LineCheckItemEntity itemEntity = lineCheckItemRepository.findById(itemDto.getId())
                        .orElseThrow(() -> new RuntimeException("LineCheckItem not found: " + itemDto.getId()));

                if (itemEntity.getLineCheckStation() == null
                        || !stationEntity.getId().equals(itemEntity.getLineCheckStation().getId())) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Line check item does not belong to line check station "
                                    + stationEntity.getId()
                    );
                }

                // ✅ Update entity fields
                itemEntity.setItemChecked(itemDto.isItemChecked());
                itemEntity.setChecked(itemDto.isItemChecked());
                itemEntity.setMissing(Boolean.TRUE.equals(itemDto.getMissing()));

                if (Boolean.TRUE.equals(itemDto.getMissing())) {
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

                // A null collection identifies an older iPad payload. Preserve its
                // legacy save behavior until that client begins sending criteria.
                if (itemDto.getCriterionResponses() != null) {
                    applyCriterionResponses(itemEntity, itemDto.getCriterionResponses());
                    validateCriterionResponses(itemEntity);
                }

                itemEntity.setRequiresCorrection(requiresCorrection(itemEntity));
                applyCorrectionUpdate(itemEntity, itemDto);

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
        dto.setItemType(item.getItemType() == null ? ItemType.FOOD_PREP : item.getItemType());
        dto.setShelfLife(item.getShelfLife());
        dto.setPanSize(item.getPanSize());
        dto.setTool(item.getIsTool());
        dto.setToolName(item.getToolName());
        dto.setPortioned(item.getIsPortioned());
        dto.setPortionSize(item.getPortionSize());
        dto.setCheckMark(item.getIsCheckMark());  // ✅ template flag
        dto.setMinTemp(e.getMinTemp() != null ? e.getMinTemp() : item.getMinTemp());
        dto.setMaxTemp(e.getMaxTemp() != null ? e.getMaxTemp() : item.getMaxTemp());
        dto.setTemplateNotes(item.getTemplateNotes());
        dto.setSortOrder(item.getSortOrder());

        // User-entered fields (important!)
        dto.setItemChecked(e.isItemChecked());   // ✅ actual user check
        dto.setTempTaken(item.getIsTempTaken());    // ✅ can stay from template
        dto.setTemperature(e.getTemperature());  // ✅ user-entered
        dto.setObservations(e.getObservations()); // ✅ user-entered
        dto.setMissing(e.isMissing());
        dto.setIsCorrected(Boolean.TRUE.equals(e.getIsCorrected()));
        dto.setCorrected(Boolean.TRUE.equals(e.getIsCorrected()));
        dto.setCorrectiveNotes(e.getCorrectiveNotes());
        dto.setCorrectedAt(e.getCorrectedAt());
        dto.setCorrectedBy(e.getCorrectedBy());
        if (e.getCorrectedBy() != null) {
            dto.setCorrectedByName(userRepository.findById(e.getCorrectedBy())
                    .map(UserEntity::getUserName)
                    .orElse(null));
        }
        dto.setCriterionResponses(e.getCriterionResponses()
                .stream()
                .map(response -> convertCriterionResponseToDto(e, response))
                .toList());

        return dto;
    }

    private void applyCorrectionUpdate(
            LineCheckItemEntity entity,
            LineCheckItemDto dto
    ) {
        Boolean corrected = dto.getIsCorrected();
        if (corrected == null) {
            corrected = dto.getCorrected();
        }

        // An omitted correction flag is an older-client payload. Preserve the
        // existing status and audit data instead of accidentally reopening it.
        if (corrected == null) {
            if (dto.getCorrectiveNotes() != null) {
                entity.setCorrectiveNotes(dto.getCorrectiveNotes());
            }
            return;
        }

        boolean wasCorrected = Boolean.TRUE.equals(entity.getIsCorrected());
        boolean isNowCorrected = Boolean.TRUE.equals(corrected);

        entity.setIsCorrected(isNowCorrected);
        entity.setCorrectiveNotes(dto.getCorrectiveNotes());

        if (isNowCorrected && !wasCorrected) {
            UUID currentUserId = UserContext.getCurrentUser();
            if (currentUserId == null) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "An authenticated user is required to correct an item"
                );
            }
            entity.setCorrectedBy(currentUserId);
            entity.setCorrectedAt(Instant.now());
        } else if (!isNowCorrected) {
            entity.setCorrectedBy(null);
            entity.setCorrectedAt(null);
        }
    }

    private LineCheckCriterionResponseDto convertCriterionResponseToDto(
            LineCheckItemEntity item,
            LineCheckCriterionResponseEntity response
    ) {
        LineCheckCriterionResponseDto dto = new LineCheckCriterionResponseDto();
        dto.setId(response.getId());
        dto.setItemCriterionId(response.getItemCriterionId());
        dto.setLabel(response.getLabel());
        dto.setResponseType(response.getResponseType());
        dto.setRequired(response.getRequired());
        dto.setRequireNotesOnFailure(response.getRequireNotesOnFailure());
        dto.setMinValue(response.getMinValue());
        dto.setMaxValue(response.getMaxValue());
        dto.setUnit(response.getUnit());
        dto.setSortOrder(response.getSortOrder());
        dto.setBooleanValue(response.getBooleanValue());
        dto.setNumberValue(response.getNumberValue());
        dto.setTextValue(response.getTextValue());
        dto.setNotes(response.getNotes());
        dto.setFailed(isFailure(response));
        dto.setPhotoIds(item.getPhotos()
                .stream()
                .filter(photo -> response.getId() != null
                        && response.getId().equals(photo.getCriterionResponseId()))
                .map(LineCheckPhotoEntity::getId)
                .toList());
        return dto;
    }

    private void applyCriterionResponses(
            LineCheckItemEntity item,
            List<LineCheckCriterionResponseDto> submittedResponses
    ) {
        Map<UUID, LineCheckCriterionResponseEntity> byId = item.getCriterionResponses()
                .stream()
                .filter(response -> response.getId() != null)
                .collect(Collectors.toMap(
                        LineCheckCriterionResponseEntity::getId,
                        response -> response
                ));

        Set<UUID> submittedIds = new HashSet<>();
        for (LineCheckCriterionResponseDto submitted : submittedResponses) {
            if (submitted.getId() == null || !submittedIds.add(submitted.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Each criterion response must have a unique response ID"
                );
            }

            LineCheckCriterionResponseEntity response = byId.get(submitted.getId());
            if (response == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Criterion response does not belong to line check item " + item.getId()
                );
            }

            switch (response.getResponseType()) {
                case PASS_FAIL, CHECKBOX -> {
                    response.setBooleanValue(submitted.getBooleanValue());
                    response.setNumberValue(null);
                    response.setTextValue(null);
                }
                case TEMPERATURE, NUMBER -> {
                    response.setBooleanValue(null);
                    response.setNumberValue(submitted.getNumberValue());
                    response.setTextValue(null);
                }
                case TEXT -> {
                    response.setBooleanValue(null);
                    response.setNumberValue(null);
                    response.setTextValue(submitted.getTextValue());
                }
                case PHOTO -> {
                    response.setBooleanValue(null);
                    response.setNumberValue(null);
                    response.setTextValue(null);
                }
            }
            response.setNotes(submitted.getNotes());
        }
    }

    private void validateCriterionResponses(LineCheckItemEntity item) {
        if (item.isMissing()) {
            return;
        }

        for (LineCheckCriterionResponseEntity response : item.getCriterionResponses()) {
            if (Boolean.TRUE.equals(response.getRequired()) && !hasAnswer(item, response)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Required criterion is unanswered: " + response.getLabel()
                );
            }

            if (Boolean.TRUE.equals(response.getRequireNotesOnFailure())
                    && isFailure(response)
                    && (response.getNotes() == null || response.getNotes().isBlank())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Notes are required when criterion fails: " + response.getLabel()
                );
            }
        }
    }

    private boolean hasAnswer(
            LineCheckItemEntity item,
            LineCheckCriterionResponseEntity response
    ) {
        return switch (response.getResponseType()) {
            case PASS_FAIL, CHECKBOX -> response.getBooleanValue() != null;
            case TEMPERATURE, NUMBER -> response.getNumberValue() != null;
            case TEXT -> response.getTextValue() != null && !response.getTextValue().isBlank();
            case PHOTO -> item.getPhotos().stream()
                    .anyMatch(photo -> response.getId() != null
                            && response.getId().equals(photo.getCriterionResponseId()));
        };
    }

    private boolean isFailure(LineCheckCriterionResponseEntity response) {
        if ((response.getResponseType() == ResponseType.PASS_FAIL
                || response.getResponseType() == ResponseType.CHECKBOX)
                && response.getBooleanValue() != null) {
            return !response.getBooleanValue();
        }

        if ((response.getResponseType() == ResponseType.TEMPERATURE
                || response.getResponseType() == ResponseType.NUMBER)
                && response.getNumberValue() != null) {
            return (response.getMinValue() != null
                    && response.getNumberValue() < response.getMinValue())
                    || (response.getMaxValue() != null
                    && response.getNumberValue() > response.getMaxValue());
        }

        return false;
    }

    private boolean requiresCorrection(LineCheckItemEntity item) {
        if (item.isMissing()) {
            return true;
        }

        boolean legacyCheckFailed = Boolean.TRUE.equals(item.getItem().getIsCheckMark())
                && !item.isChecked();
        boolean legacyTemperatureFailed = item.getTemperature() != null
                && ((item.getMinTemp() != null && item.getTemperature() < item.getMinTemp())
                || (item.getMaxTemp() != null && item.getTemperature() > item.getMaxTemp()));
        boolean criterionFailed = item.getCriterionResponses()
                .stream()
                .anyMatch(this::isFailure);

        return legacyCheckFailed || legacyTemperatureFailed || criterionFailed;
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
//        Instant last30Days =
//                today.minusDays(30)
//                        .atStartOfDay(zone)
//                        .toInstant();
        Instant start = today.minusDays(30).atStartOfDay(zone).toInstant();
        Instant end = now; // Instant.now() in same zone conversion if needed

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
                        lineCheckItemRepository.missingItemsByWeekday(locationId, start, end)
                )
        );

        dto.setMostOutOfTempDay(
                extractTopDay(
                        lineCheckItemRepository.outOfTempByWeekday(locationId, start, end)
                )
        );

        dto.setMostIncorrectPrepDay(
                extractTopDay(
                        lineCheckItemRepository.incorrectPrepByWeekday(locationId, start, end)
                )
        );

        dto.setWeakestLineCheckDay(
                extractTopDay(
                        lineCheckRepository.weakestCheckDays(locationId, start, end)
                )
        );

        // -------------------------------
// Top 3 weekday analytics (last 30 days)
// -------------------------------

        dto.setTopMissingDays(
                mapRankedDays(
                        lineCheckItemRepository.topMissingDays(locationId, start, end)
                )
        );

        dto.setTopOutOfTempDays(
                mapRankedDays(
                        lineCheckItemRepository.topOutOfTempDays(locationId, start, end)
                )
        );

        dto.setTopIncorrectPrepDays(
                mapRankedDays(
                        lineCheckItemRepository.topIncorrectPrepDays(locationId, start, end)
                )
        );

        dto.setTopWeakestCompletionDays(
                mapRankedDays(
                        lineCheckItemRepository.topWeakestCompletionDays(locationId, start, end)
                )
        );


// -------------------------------
// Top 5 issue items (last 30 days)
// -------------------------------

        dto.setTopMissingItems(
                mapRankedItems(
                        lineCheckItemRepository.topMissingItems(locationId, start, end)
                )
        );

        dto.setTopOutOfTempItems(
                mapRankedItems(
                        lineCheckItemRepository.topOutOfTempItems(locationId, start, end)
                )
        );

        dto.setTopIncorrectPrepItems(
                mapRankedItems(
                        lineCheckItemRepository.topIncorrectPrepItems(locationId, start, end)
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
        Object[] row = results.get(0);
        if (row[0] == null) return "N/A";
        // Return short day name (Mon, Tue, etc.)
        return row[0].toString().trim();
    }

    private RankedDayDto[] mapRankedDays(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new RankedDayDto(
                        (String) r[0],
                        ((Number) r[1]).doubleValue()
                ))
                .toArray(RankedDayDto[]::new);
    }

    private RankedItemDto[] mapRankedItems(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new RankedItemDto(
                        (String) r[0],
                        ((Number) r[1]).longValue()
                ))
                .toArray(RankedItemDto[]::new);
    }



}
