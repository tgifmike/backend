package com.backend.backend.controller;

import com.backend.backend.dto.DashboardMetricsDto;
import com.backend.backend.dto.LineCheckDto;
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
public class LineCheckController {

    private final LineCheckService lineCheckService;
    private final UserRepository userRepository;
    private final StationRepository stationRepository;

        // ---------------------------------------------------------
        // CREATE INITIAL LINE CHECK (before user fills items)
        // ---------------------------------------------------------
        @PostMapping("/create")
        public ResponseEntity<LineCheckDto> createLineCheck(
                @RequestParam("userId") UUID userId,
                @RequestBody List<UUID> stationIds
        ) {
            LineCheckDto saved = lineCheckService.createLineCheck(userId, stationIds);
            return ResponseEntity.ok(saved);
        }


    // ---------------------------------------------------------
        // GET ALL LINE CHECKS (DTO)
        // ---------------------------------------------------------
        @GetMapping("/getAllLineChecks")
        public ResponseEntity<List<LineCheckDto>> getAllLineChecks() {
            return ResponseEntity.ok(lineCheckService.getAllLineChecksDto());
        }

        // ---------------------------------------------------------
        // GET SINGLE LINE CHECK
        // ---------------------------------------------------------
        @GetMapping("/{id}")
        public ResponseEntity<LineCheckDto> getLineCheck(@PathVariable UUID id) {
            return ResponseEntity.ok(lineCheckService.getLineCheckDtoById(id));
        }






    // ---------------------------------------------------------
        // SAVE COMPLETED LINE CHECK (mobile app submit)
        // ---------------------------------------------------------
    @PostMapping("/save")
    public ResponseEntity<LineCheckDto> saveLineCheck(@RequestBody LineCheckDto dto) {
        LineCheckDto saved = lineCheckService.saveLineCheck(dto);
        return ResponseEntity.ok(saved);
    }

        //get linecheck complted and by locaion
        @GetMapping("/completed/by-location/{locationId}")
        public ResponseEntity<List<LineCheckDto>> getCompletedLineChecksByLocation(@PathVariable UUID locationId) {
            List<LineCheckDto> lineChecks = lineCheckService.getCompletedLineChecksByLocation(locationId);
            return ResponseEntity.ok(lineChecks);
        }


    // ---------------------------------------------------------
    // dashboard endpoint
    // ---------------------------------------------------------

    @GetMapping("/dashboard/{locationId}")
    public ResponseEntity<DashboardMetricsDto> getDashboardMetrics(@PathVariable UUID locationId) {
        try {
            DashboardMetricsDto metrics = lineCheckService.getDashboardMetrics(locationId);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            e.printStackTrace(); // logs full error to Heroku console
            return ResponseEntity.status(500).body(null);
        }
    }

}




