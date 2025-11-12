package com.backend.backend.controller;

import com.backend.backend.dto.LineCheckDto;
import com.backend.backend.entity.LineCheckEntity;
import com.backend.backend.entity.StationEntity;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.service.LineCheckService;
import com.backend.backend.repositories.StationRepository;
import com.backend.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;



@RestController
@RequestMapping("/line-checks")
@RequiredArgsConstructor
//@CrossOrigin(origins = "*") // allow requests from frontend domain
public class LineCheckController {

    private final LineCheckService lineCheckService;
    private final UserRepository userRepository;
    private final StationRepository stationRepository;

    // ✅ Create a new line check
    @PostMapping("/create")
    public LineCheckEntity createLineCheck(
            @RequestParam("userId") UUID userId,
            @RequestBody List<UUID> stationIds
    ) {
        System.out.println("POST /line-checks/create called with userId=" + userId + ", stationIds=" + stationIds);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<StationEntity> stations = stationRepository.findAllById(stationIds);
        return lineCheckService.createLineCheck(user, stations);
    }

    // ✅ Fetch all line checks
    @GetMapping("/getAllLineChecks")
    public ResponseEntity<List<LineCheckDto>> getAllLineChecks() {
        return ResponseEntity.ok(lineCheckService.getAllLineChecksDto());
    }

    // ✅ Get line check by ID
    @GetMapping("/{id}")
    public LineCheckEntity getLineCheck(@PathVariable UUID id) {
        return lineCheckService.getLineCheckById(id);
    }

    @PostMapping("/save")
    public ResponseEntity<LineCheckDto> saveLineCheck(@RequestBody LineCheckDto lineCheckDto) {
        LineCheckEntity saved = lineCheckService.saveLineCheck(lineCheckDto);
        return ResponseEntity.ok(new LineCheckDto(
                saved.getId(),
                saved.getUser() != null ? saved.getUser().getUserName() : "Unknown",
                saved.getCheckTime(),
                lineCheckDto.stations() // or map again if you want full updated data
        ));
    }
}


