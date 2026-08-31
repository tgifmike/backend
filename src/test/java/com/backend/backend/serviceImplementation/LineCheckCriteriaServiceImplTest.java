package com.backend.backend.serviceImplementation;

import com.backend.backend.dto.LineCheckCriterionResponseDto;
import com.backend.backend.dto.LineCheckDto;
import com.backend.backend.dto.LineCheckItemCorrectionRequestDto;
import com.backend.backend.dto.LineCheckItemDto;
import com.backend.backend.dto.LineCheckStationDto;
import com.backend.backend.entity.ItemCriterionEntity;
import com.backend.backend.entity.ItemEntity;
import com.backend.backend.entity.LineCheckCriterionResponseEntity;
import com.backend.backend.entity.LineCheckEntity;
import com.backend.backend.entity.LineCheckItemEntity;
import com.backend.backend.entity.LineCheckStationEntity;
import com.backend.backend.entity.StationEntity;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.enums.ItemType;
import com.backend.backend.enums.ResponseType;
import com.backend.backend.repositories.LineCheckItemRepository;
import com.backend.backend.repositories.LineCheckRepository;
import com.backend.backend.repositories.LineCheckStationRepository;
import com.backend.backend.repositories.LocationRepository;
import com.backend.backend.repositories.StationRepository;
import com.backend.backend.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LineCheckCriteriaServiceImplTest {

    @Test
    void updateCorrectionStoresCurrentUserAndReturnsAuditDetails() {
        UUID itemId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        LineCheckItemEntity item = new LineCheckItemEntity();
        item.setId(itemId);
        item.setItem(item(ItemType.EQUIPMENT));
        item.setPhotos(new ArrayList<>());
        item.setCriterionResponses(new ArrayList<>());

        UserEntity currentUser = new UserEntity();
        currentUser.setId(currentUserId);
        currentUser.setUserName("Closing Manager");

        LineCheckServiceImpl service = service(
                unsupported(LineCheckRepository.class),
                proxy(UserRepository.class, findById(currentUserId, currentUser)),
                unsupported(StationRepository.class),
                unsupported(LineCheckStationRepository.class),
                proxy(LineCheckItemRepository.class, findByIdAndSave(itemId, item))
        );

        Instant beforeUpdate = Instant.now();
        LineCheckItemDto result = service.updateCorrection(
                itemId,
                new LineCheckItemCorrectionRequestDto(
                        true,
                        "Reheated to the required temperature"
                ),
                currentUser
        );

        assertThat(item.getIsCorrected()).isTrue();
        assertThat(item.getCorrectiveNotes())
                .isEqualTo("Reheated to the required temperature");
        assertThat(item.getCorrectedBy()).isEqualTo(currentUserId);
        assertThat(item.getCorrectedAt()).isAfterOrEqualTo(beforeUpdate);
        assertThat(result.getCorrected()).isTrue();
        assertThat(result.getCorrectedBy()).isEqualTo(currentUserId);
        assertThat(result.getCorrectedByName()).isEqualTo("Closing Manager");
        assertThat(result.getCorrectedAt()).isNotNull();
    }

    @Test
    void updateCorrectionPreservesAuditDetailsWhenMarkedUncorrected() {
        UUID itemId = UUID.randomUUID();
        UUID originalCorrectingUserId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        Instant originalCorrectedAt = Instant.now().minusSeconds(300);

        LineCheckItemEntity item = new LineCheckItemEntity();
        item.setId(itemId);
        item.setItem(item(ItemType.EQUIPMENT));
        item.setPhotos(new ArrayList<>());
        item.setCriterionResponses(new ArrayList<>());
        item.setIsCorrected(true);
        item.setCorrectedBy(originalCorrectingUserId);
        item.setCorrectedAt(originalCorrectedAt);

        UserEntity originalCorrectingUser = new UserEntity();
        originalCorrectingUser.setId(originalCorrectingUserId);
        originalCorrectingUser.setUserName("Opening Manager");

        UserEntity currentUser = new UserEntity();
        currentUser.setId(currentUserId);

        LineCheckServiceImpl service = service(
                unsupported(LineCheckRepository.class),
                proxy(
                        UserRepository.class,
                        findById(originalCorrectingUserId, originalCorrectingUser)
                ),
                unsupported(StationRepository.class),
                unsupported(LineCheckStationRepository.class),
                proxy(LineCheckItemRepository.class, findByIdAndSave(itemId, item))
        );

        LineCheckItemDto result = service.updateCorrection(
                itemId,
                new LineCheckItemCorrectionRequestDto(false, "Needs another check"),
                currentUser
        );

        assertThat(item.getIsCorrected()).isFalse();
        assertThat(item.getCorrectedBy()).isEqualTo(originalCorrectingUserId);
        assertThat(item.getCorrectedAt()).isEqualTo(originalCorrectedAt);
        assertThat(result.getCorrected()).isFalse();
        assertThat(result.getCorrectedBy()).isEqualTo(originalCorrectingUserId);
        assertThat(result.getCorrectedByName()).isEqualTo("Opening Manager");
        assertThat(result.getCorrectedAt()).isEqualTo(originalCorrectedAt);
    }

    @Test
    void createLineCheckSnapshotsOnlyActiveCriteria() {
        UUID userId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUserName("iPad User");

        StationEntity station = new StationEntity();
        station.setId(stationId);
        station.setStationName("Restrooms");

        ItemEntity item = item(ItemType.CLEANLINESS);
        item.setStation(station);
        item.setCriteria(new ArrayList<>(List.of(
                criterion(item, "Floor clean and dry?", true),
                criterion(item, "Retired question", false)
        )));
        station.setItems(new ArrayList<>(List.of(item)));

        UserRepository userRepository = proxy(
                UserRepository.class,
                findById(userId, user)
        );
        StationRepository stationRepository = proxy(
                StationRepository.class,
                findById(stationId, station)
        );
        LineCheckRepository lineCheckRepository = proxy(
                LineCheckRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("save")) {
                        LineCheckEntity lineCheck = (LineCheckEntity) args[0];
                        lineCheck.setId(UUID.randomUUID());
                        return lineCheck;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        LineCheckStationRepository stationCheckRepository = proxy(
                LineCheckStationRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("save")) {
                        LineCheckStationEntity stationCheck = (LineCheckStationEntity) args[0];
                        stationCheck.setId(UUID.randomUUID());
                        stationCheck.getLineCheckItems().forEach(lineCheckItem -> {
                            lineCheckItem.setId(UUID.randomUUID());
                            lineCheckItem.getCriterionResponses()
                                    .forEach(response -> response.setId(UUID.randomUUID()));
                        });
                        return stationCheck;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );

        LineCheckServiceImpl service = service(
                lineCheckRepository,
                userRepository,
                stationRepository,
                stationCheckRepository,
                unsupported(LineCheckItemRepository.class)
        );

        LineCheckDto result = service.createLineCheck(userId, List.of(stationId));
        LineCheckItemDto itemDto = result.getStations().getFirst().getItems().getFirst();

        assertThat(itemDto.getItemType()).isEqualTo(ItemType.CLEANLINESS);
        assertThat(itemDto.getCriterionResponses()).singleElement().satisfies(response -> {
            assertThat(response.getLabel()).isEqualTo("Floor clean and dry?");
            assertThat(response.getResponseType()).isEqualTo(ResponseType.PASS_FAIL);
            assertThat(response.getRequired()).isTrue();
        });
    }

    @Test
    void saveRejectsAnUnansweredRequiredCriterion() {
        UUID lineCheckId = UUID.randomUUID();
        UUID stationCheckId = UUID.randomUUID();
        UUID lineCheckItemId = UUID.randomUUID();
        UUID responseId = UUID.randomUUID();

        LineCheckEntity lineCheck = new LineCheckEntity();
        lineCheck.setId(lineCheckId);

        LineCheckStationEntity stationCheck = new LineCheckStationEntity();
        stationCheck.setId(stationCheckId);
        stationCheck.setLineCheck(lineCheck);

        ItemEntity item = item(ItemType.EQUIPMENT);
        LineCheckItemEntity lineCheckItem = new LineCheckItemEntity();
        lineCheckItem.setId(lineCheckItemId);
        lineCheckItem.setLineCheckStation(stationCheck);
        lineCheckItem.setItem(item);
        lineCheckItem.setPhotos(new ArrayList<>());

        LineCheckCriterionResponseEntity response = LineCheckCriterionResponseEntity.builder()
                .id(responseId)
                .lineCheckItem(lineCheckItem)
                .label("Cooler temperature")
                .responseType(ResponseType.TEMPERATURE)
                .required(true)
                .build();
        lineCheckItem.setCriterionResponses(new ArrayList<>(List.of(response)));

        LineCheckCriterionResponseDto responseDto = new LineCheckCriterionResponseDto();
        responseDto.setId(responseId);
        LineCheckItemDto itemDto = new LineCheckItemDto();
        itemDto.setId(lineCheckItemId);
        itemDto.setMissing(false);
        itemDto.setCriterionResponses(List.of(responseDto));
        LineCheckStationDto stationDto = new LineCheckStationDto();
        stationDto.setId(stationCheckId);
        stationDto.setItems(List.of(itemDto));
        LineCheckDto lineCheckDto = new LineCheckDto();
        lineCheckDto.setId(lineCheckId);
        lineCheckDto.setStations(List.of(stationDto));

        LineCheckServiceImpl service = service(
                proxy(LineCheckRepository.class, findById(lineCheckId, lineCheck)),
                unsupported(UserRepository.class),
                unsupported(StationRepository.class),
                proxy(
                        LineCheckStationRepository.class,
                        findById(stationCheckId, stationCheck)
                ),
                proxy(
                        LineCheckItemRepository.class,
                        findById(lineCheckItemId, lineCheckItem)
                )
        );

        assertThatThrownBy(() -> service.saveLineCheck(lineCheckDto))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason())
                            .contains("Required criterion is unanswered");
                });
    }

    private static LineCheckServiceImpl service(
            LineCheckRepository lineCheckRepository,
            UserRepository userRepository,
            StationRepository stationRepository,
            LineCheckStationRepository stationCheckRepository,
            LineCheckItemRepository lineCheckItemRepository
    ) {
        return new LineCheckServiceImpl(
                lineCheckRepository,
                userRepository,
                stationRepository,
                stationCheckRepository,
                lineCheckItemRepository,
                unsupported(LocationRepository.class)
        );
    }

    private static ItemEntity item(ItemType itemType) {
        ItemEntity item = new ItemEntity();
        item.setId(UUID.randomUUID());
        item.setItemName("Inspection item");
        item.setItemType(itemType);
        item.setIsTool(false);
        item.setIsPortioned(false);
        item.setIsTempTaken(false);
        item.setIsCheckMark(false);
        item.setSortOrder(0);
        return item;
    }

    private static ItemCriterionEntity criterion(
            ItemEntity item,
            String label,
            boolean active
    ) {
        return ItemCriterionEntity.builder()
                .id(UUID.randomUUID())
                .item(item)
                .label(label)
                .responseType(ResponseType.PASS_FAIL)
                .required(true)
                .sortOrder(0)
                .active(active)
                .build();
    }

    private static InvocationHandler findById(UUID expectedId, Object result) {
        return (proxy, method, args) -> {
            if (method.getName().equals("findById") && expectedId.equals(args[0])) {
                return Optional.of(result);
            }
            throw new UnsupportedOperationException(method.getName());
        };
    }

    private static InvocationHandler findByIdAndSave(UUID expectedId, Object result) {
        return (proxy, method, args) -> {
            if (method.getName().equals("findById") && expectedId.equals(args[0])) {
                return Optional.of(result);
            }
            if (method.getName().equals("save")) {
                return args[0];
            }
            throw new UnsupportedOperationException(method.getName());
        };
    }

    private static <T> T unsupported(Class<T> type) {
        return proxy(
                type,
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                handler
        );
    }
}
