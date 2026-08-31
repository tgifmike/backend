package com.backend.backend.controller;

import com.backend.backend.config.UserContext;
import com.backend.backend.dto.LineCheckItemCorrectionRequestDto;
import com.backend.backend.dto.LineCheckItemDto;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.repositories.UserRepository;
import com.backend.backend.service.LineCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/line-check-items")
@RequiredArgsConstructor
public class LineCheckItemController {

    private final LineCheckService lineCheckService;
    private final UserRepository userRepository;

    @PatchMapping("/{itemId}/correction")
    public LineCheckItemDto updateCorrection(
            @PathVariable UUID itemId,
            @RequestBody LineCheckItemCorrectionRequestDto request
    ) {
        UUID currentUserId = UserContext.getCurrentUser();
        if (currentUserId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "An authenticated user is required to update a correction"
            );
        }

        UserEntity currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));

        return lineCheckService.updateCorrection(itemId, request, currentUser);
    }
}
