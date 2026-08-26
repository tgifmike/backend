package com.backend.backend.serviceImplementation;

import com.backend.backend.dto.ItemCriterionDto;
import com.backend.backend.dto.ItemCriterionRequestDto;
import com.backend.backend.entity.ItemCriterionEntity;
import com.backend.backend.entity.ItemEntity;
import com.backend.backend.enums.ResponseType;
import com.backend.backend.repositories.ItemCriterionRepository;
import com.backend.backend.repositories.ItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemCriterionServiceImplTest {

    @Test
    void createAssociatesCriterionWithItemAndAppliesDefaults() {
        UUID itemId = UUID.randomUUID();
        UUID criterionId = UUID.randomUUID();
        ItemEntity item = new ItemEntity();
        item.setId(itemId);

        ItemCriterionRepository criterionRepository = proxy(
                ItemCriterionRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByItem_IdOrderBySortOrderAscLabelAsc" -> List.of();
                    case "save" -> {
                        ItemCriterionEntity criterion = (ItemCriterionEntity) args[0];
                        criterion.setId(criterionId);
                        yield criterion;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
        ItemRepository itemRepository = itemRepository(itemId, item);

        ItemCriterionServiceImpl service = new ItemCriterionServiceImpl(
                criterionRepository,
                itemRepository
        );
        ItemCriterionRequestDto request = new ItemCriterionRequestDto();
        request.setLabel("  Floor clean and dry?  ");
        request.setResponseType(ResponseType.PASS_FAIL);

        ItemCriterionDto result = service.create(itemId, request);

        assertThat(result.id()).isEqualTo(criterionId);
        assertThat(result.itemId()).isEqualTo(itemId);
        assertThat(result.label()).isEqualTo("Floor clean and dry?");
        assertThat(result.required()).isFalse();
        assertThat(result.requireNotesOnFailure()).isFalse();
        assertThat(result.active()).isTrue();
        assertThat(result.sortOrder()).isZero();
    }

    @Test
    void createRejectsNumericLimitsForPassFailCriterion() {
        UUID itemId = UUID.randomUUID();
        ItemEntity item = new ItemEntity();
        item.setId(itemId);
        AtomicBoolean saved = new AtomicBoolean();

        ItemCriterionRepository criterionRepository = proxy(
                ItemCriterionRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("save")) {
                        saved.set(true);
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        ItemCriterionServiceImpl service = new ItemCriterionServiceImpl(
                criterionRepository,
                itemRepository(itemId, item)
        );
        ItemCriterionRequestDto request = new ItemCriterionRequestDto();
        request.setLabel("Door seals intact?");
        request.setResponseType(ResponseType.PASS_FAIL);
        request.setMinValue(1.0);

        assertThatThrownBy(() -> service.create(itemId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("only valid for numeric criteria");
        assertThat(saved).isFalse();
    }

    private static ItemRepository itemRepository(UUID itemId, ItemEntity item) {
        return proxy(
                ItemRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("findById") && itemId.equals(args[0])) {
                        return Optional.of(item);
                    }
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
